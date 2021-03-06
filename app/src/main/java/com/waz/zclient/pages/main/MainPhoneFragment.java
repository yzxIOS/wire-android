/**
 * Wire
 * Copyright (C) 2016 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.zclient.pages.main;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.waz.api.ErrorsList;
import com.waz.api.IConversation;
import com.waz.api.Message;
import com.waz.api.User;
import com.waz.model.MessageData;
import com.waz.utils.wrappers.URI;
import com.waz.zclient.BaseActivity;
import com.waz.zclient.OnBackPressedListener;
import com.waz.zclient.R;
import com.waz.zclient.controllers.accentcolor.AccentColorObserver;
import com.waz.zclient.controllers.collections.CollectionsObserver;
import com.waz.zclient.controllers.confirmation.ConfirmationObserver;
import com.waz.zclient.controllers.confirmation.ConfirmationRequest;
import com.waz.zclient.controllers.confirmation.IConfirmationController;
import com.waz.zclient.controllers.giphy.GiphyObserver;
import com.waz.zclient.controllers.navigation.Page;
import com.waz.zclient.controllers.onboarding.OnboardingControllerObserver;
import com.waz.zclient.controllers.singleimage.SingleImageObserver;
import com.waz.zclient.conversation.CollectionController;
import com.waz.zclient.conversation.CollectionFragment;
import com.waz.zclient.conversation.ShareToMultipleFragment;
import com.waz.zclient.core.stores.inappnotification.InAppNotificationStoreObserver;
import com.waz.zclient.core.stores.inappnotification.KnockingEvent;
import com.waz.zclient.pages.BaseFragment;
import com.waz.zclient.pages.main.backgroundmain.views.BackgroundFrameLayout;
import com.waz.zclient.pages.main.conversation.SingleImageFragment;
import com.waz.zclient.pages.main.conversation.SingleImageMessageFragment;
import com.waz.zclient.pages.main.conversation.SingleImageUserFragment;
import com.waz.zclient.pages.main.conversation.VideoPlayerFragment;
import com.waz.zclient.pages.main.conversationlist.ConfirmationFragment;
import com.waz.zclient.pages.main.conversationpager.ConversationPagerFragment;
import com.waz.zclient.pages.main.giphy.GiphySharingPreviewFragment;
import com.waz.zclient.pages.main.onboarding.OnBoardingHintFragment;
import com.waz.zclient.pages.main.onboarding.OnBoardingHintType;
import com.waz.zclient.utils.SyncErrorUtils;
import com.waz.zclient.utils.ViewUtils;
import com.waz.zclient.views.menus.ConfirmationMenu;
import net.hockeyapp.android.ExceptionHandler;

public class MainPhoneFragment extends BaseFragment<MainPhoneFragment.Container> implements OnBackPressedListener,
                                                                                            ConversationPagerFragment.Container,
                                                                                            OnBoardingHintFragment.Container,
                                                                                            OnboardingControllerObserver,
                                                                                            SingleImageObserver,
                                                                                            SingleImageFragment.Container,
                                                                                            GiphyObserver,
                                                                                            ConfirmationObserver,
                                                                                            AccentColorObserver,
                                                                                            CollectionsObserver,
                                                                                            ConfirmationFragment.Container,
                                                                                            InAppNotificationStoreObserver {

    public static final String TAG = MainPhoneFragment.class.getName();
    private ConfirmationMenu confirmationMenu;
    private BackgroundFrameLayout backgroundLayout;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    //  LifeCycle
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        if (savedInstanceState == null) {
            getChildFragmentManager().beginTransaction().replace(R.id.fl_fragment_main_content,
                                                                 ConversationPagerFragment.newInstance(),
                                                                 ConversationPagerFragment.TAG).commit();
        }

        backgroundLayout = ViewUtils.getView(view, R.id.bl__background);
        confirmationMenu = ViewUtils.getView(view, R.id.cm__confirm_action_light);
        confirmationMenu.setVisibility(View.GONE);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        getStoreFactory().getInAppNotificationStore().addInAppNotificationObserver(this);
        getControllerFactory().getSingleImageController().addSingleImageObserver(this);
        getControllerFactory().getOnboardingController().addOnboardingControllerObserver(this);
        getControllerFactory().getGiphyController().addObserver(this);
        getControllerFactory().getConfirmationController().addConfirmationObserver(this);
        getControllerFactory().getAccentColorController().addAccentColorObserver(this);
        getCollectionController().addObserver(this);

        getControllerFactory().getAccentColorController().addAccentColorObserver(backgroundLayout);
        getControllerFactory().getBackgroundController().addBackgroundObserver(backgroundLayout);

        OnBoardingHintFragment fragment = (OnBoardingHintFragment) getChildFragmentManager().findFragmentByTag(
            OnBoardingHintFragment.TAG);
        if (fragment != null) {
            getControllerFactory().getOnboardingController().setCurrentHintType(fragment.getOnBoardingHintType());
        }
    }

    @Override
    public void onStop() {
        getControllerFactory().getGiphyController().removeObserver(this);
        getControllerFactory().getSingleImageController().removeSingleImageObserver(this);
        getControllerFactory().getOnboardingController().removeOnboardingControllerObserver(this);
        getControllerFactory().getConfirmationController().removeConfirmationObserver(this);
        getControllerFactory().getAccentColorController().removeAccentColorObserver(this);
        getStoreFactory().getInAppNotificationStore().removeInAppNotificationObserver(this);
        getCollectionController().removeObserver(this);

        getControllerFactory().getAccentColorController().removeAccentColorObserver(backgroundLayout);
        getControllerFactory().getBackgroundController().removeBackgroundObserver(backgroundLayout);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        confirmationMenu = null;
        super.onDestroyView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        getChildFragmentManager().findFragmentById(R.id.fl_fragment_main_content).onActivityResult(requestCode,
                                                                                                   resultCode,
                                                                                                   data);
    }

    private CollectionController getCollectionController() {
        return ((BaseActivity) getActivity()).injectJava(CollectionController.class);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    //  Notifications
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    public boolean onBackPressed() {
        if (confirmationMenu.getVisibility() == View.VISIBLE) {
            confirmationMenu.animateToShow(false);
            return true;
        }

        // Clear any overlays
        dismissOnBoardingHint(OnBoardingHintType.NONE);

        if (getChildFragmentManager().getBackStackEntryCount() > 0) {
            Fragment topFragment = getChildFragmentManager().findFragmentByTag(getChildFragmentManager().getBackStackEntryAt(
                getChildFragmentManager().getBackStackEntryCount() - 1).getName());
            if (topFragment instanceof SingleImageFragment) {
                return ((SingleImageFragment) topFragment).onBackPressed();
            } else if (topFragment instanceof VideoPlayerFragment) {
                return ((VideoPlayerFragment) topFragment).onBackPressed();
            } else if (topFragment instanceof GiphySharingPreviewFragment) {
                if (!((GiphySharingPreviewFragment) topFragment).onBackPressed()) {
                    getChildFragmentManager().popBackStackImmediate(GiphySharingPreviewFragment.TAG,
                                                                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }
                return true;
            } else if (topFragment instanceof CollectionFragment) {
                return  ((CollectionFragment) topFragment).onBackPressed();
            } else if (topFragment instanceof ConfirmationFragment) {
                return ((ConfirmationFragment) topFragment).onBackPressed();
            }

        }

        // Back press is first delivered to the notification fragment, and if it's not consumed there,
        // it's then delivered to the main content.

        Fragment fragment = getChildFragmentManager().findFragmentById(R.id.fl_fragment_main_content);
        if (fragment instanceof OnBackPressedListener &&
            ((OnBackPressedListener) fragment).onBackPressed()) {
            return true;
        }

        fragment = getChildFragmentManager().findFragmentById(R.id.fl__overlay_container);
        if (fragment instanceof OnBackPressedListener &&
            ((OnBackPressedListener) fragment).onBackPressed()) {
            return true;
        }

        return getChildFragmentManager().popBackStackImmediate();
    }

    @Override
    public void onOpenUrl(String url) {
        getContainer().onOpenUrl(url);
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    //
    //  OnBoardingHintFragment
    //
    //////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void dismissOnBoardingHint(OnBoardingHintType requestedType) {
        getControllerFactory().getOnboardingController().hideOnboardingHint(requestedType);
    }

    @Override
    public void onShowOnboardingHint(final OnBoardingHintType hintType, int delayMilSec) {
        if (hintType == OnBoardingHintType.NONE) {
            return;
        }

        new Handler(Looper.myLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getContainer() == null || !isResumed()) {
                    return;
                }

                // Additional check if hint types match. Some animations go through conversation list and might trigger pull down hint
                Page currentPage = getControllerFactory().getNavigationController().getCurrentPage();
                IConversation currentConversation = getStoreFactory().getConversationStore().getCurrentConversation();

                boolean currentConversationHasDraft = TextUtils.isEmpty(getStoreFactory().getDraftStore().getDraft(
                    getStoreFactory().getConversationStore().getCurrentConversation()));
                OnBoardingHintType currentHintType = getControllerFactory().getOnboardingController().getCurrentOnBoardingHint(
                    currentPage,
                    currentConversation,
                    currentConversationHasDraft);
                if (hintType != currentHintType) {
                    return;
                }

                getChildFragmentManager().popBackStackImmediate(OnBoardingHintFragment.TAG,
                                                                FragmentManager.POP_BACK_STACK_INCLUSIVE);
                getChildFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.fade_in,
                                         R.anim.slide_out_to_top,
                                         R.anim.slide_in_from_bottom,
                                         R.anim.fade_out)
                    .add(R.id.fl_fragment_main_onboarding,
                         OnBoardingHintFragment.newInstance(hintType),
                         OnBoardingHintFragment.TAG)
                    .addToBackStack(OnBoardingHintFragment.TAG)
                    .commit();
            }
        }, delayMilSec);
    }

    @Override
    public void onHideOnboardingHint(OnBoardingHintType type) {
        getChildFragmentManager().popBackStackImmediate(OnBoardingHintFragment.TAG,
                                                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public void onShowSingleImage(Message message) {
        getChildFragmentManager().beginTransaction()
                                 .add(R.id.fl__overlay_container,
                                      SingleImageMessageFragment.newInstance(message),
                                      SingleImageMessageFragment.TAG)
                                 .addToBackStack(SingleImageMessageFragment.TAG)
                                 .commit();
        getControllerFactory().getNavigationController().setRightPage(Page.SINGLE_MESSAGE, TAG);
    }

    @Override
    public void onShowUserImage(User user) {
        getChildFragmentManager().beginTransaction()
                                 .add(R.id.fl__overlay_container,
                                      SingleImageUserFragment.newInstance(user),
                                      SingleImageUserFragment.TAG)
                                 .addToBackStack(SingleImageUserFragment.TAG)
                                 .commit();
        getControllerFactory().getNavigationController().setRightPage(Page.SINGLE_MESSAGE, TAG);
    }

    @Override
    public void onHideSingleImage() {

    }

    @Override
    public void onShowVideo(URI uri) {
        getChildFragmentManager().beginTransaction()
                                 .add(R.id.fl__overlay_container,
                                      VideoPlayerFragment.newInstance(uri),
                                      VideoPlayerFragment.TAG)
                                 .addToBackStack(VideoPlayerFragment.TAG)
                                 .commit();
        getControllerFactory().getNavigationController().setRightPage(Page.SINGLE_MESSAGE, TAG);
    }

    @Override
    public void onHideVideo() {
        getChildFragmentManager().popBackStackImmediate(VideoPlayerFragment.TAG,
                                                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public void onSearch(String keyword) {
        getChildFragmentManager().beginTransaction()
                                 .add(R.id.fl__overlay_container,
                                      GiphySharingPreviewFragment.newInstance(keyword),
                                      GiphySharingPreviewFragment.TAG)
                                 .addToBackStack(GiphySharingPreviewFragment.TAG)
                                 .commit();
    }

    @Override
    public void onRandomSearch() {
        getChildFragmentManager().beginTransaction()
                                 .add(R.id.fl__overlay_container,
                                      GiphySharingPreviewFragment.newInstance(),
                                      GiphySharingPreviewFragment.TAG)
                                 .addToBackStack(GiphySharingPreviewFragment.TAG)
                                 .commit();
    }

    @Override
    public void onTrendingSearch() {
        getChildFragmentManager().beginTransaction()
                                 .add(R.id.fl__overlay_container,
                                      GiphySharingPreviewFragment.newInstance(),
                                      GiphySharingPreviewFragment.TAG)
                                 .addToBackStack(GiphySharingPreviewFragment.TAG)
                                 .commit();
    }

    @Override
    public void onCloseGiphy() {
        getChildFragmentManager().popBackStackImmediate(GiphySharingPreviewFragment.TAG,
                                                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public void onCancelGiphy() {
        getChildFragmentManager().popBackStackImmediate(GiphySharingPreviewFragment.TAG,
                                                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    //
    //  CollectionsObserver
    //
    //////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void openCollection() {
    }

    @Override
    public void closeCollection() {
    }

    @Override
    public void shareCollectionItem(MessageData messageData) {
        getChildFragmentManager().beginTransaction()
                                .add(R.id.fl__overlay_container,
                                    ShareToMultipleFragment.newInstance(messageData.id()),
                                    ShareToMultipleFragment.TAG())
                                .addToBackStack(ShareToMultipleFragment.TAG())
                                .commit();
    }

    @Override
    public void closeCollectionShare() {
        getChildFragmentManager().popBackStackImmediate(ShareToMultipleFragment.TAG(),
            FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public void nextItemRequested() {

    }

    @Override
    public void previousItemRequested() {

    }

    //////////////////////////////////////////////////////////////////////////////////////////
    //
    //  ConfirmationObserver
    //
    //////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onRequestConfirmation(ConfirmationRequest confirmationRequest, @IConfirmationController.ConfirmationMenuRequester int requester) {
        confirmationMenu.onRequestConfirmation(confirmationRequest);
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    //
    //  AccentColorObserver
    //
    //////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAccentColorHasChanged(Object sender, int color) {
        if (getView() == null) {
            return;
        }
        confirmationMenu.setButtonColor(color);
    }

    @Override
    public void onIncomingMessage(Message message) {
        // ignore
    }

    @Override
    public void onIncomingKnock(KnockingEvent knock) {
        // ignore
    }

    @Override
    public void onSyncError(ErrorsList.ErrorDescription error) {
        if (getActivity() == null) {
            return;
        }

        switch (error.getType()) {
            case CANNOT_ADD_UNCONNECTED_USER_TO_CONVERSATION:
            case CANNOT_ADD_USER_TO_FULL_CONVERSATION:
            case CANNOT_CREATE_GROUP_CONVERSATION_WITH_UNCONNECTED_USER:
                getChildFragmentManager().beginTransaction()
                                         .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                                         .replace(R.id.fl_dialog_container,
                                                  ConfirmationFragment.newMessageOnlyInstance(getResources().getString(R.string.in_app_notification__sync_error__create_group_convo__title),
                                                                                              SyncErrorUtils.getGroupErrorMessage(getContext(), error),
                                                                                              getResources().getString(R.string.in_app_notification__sync_error__create_convo__button),
                                                                                              error.getId()),
                                                  ConfirmationFragment.TAG
                                                 )
                                         .addToBackStack(ConfirmationFragment.TAG)
                                         .commit();
                break;
            case CANNOT_ADD_USER_TO_FULL_CALL:
            case CANNOT_CALL_CONVERSATION_WITH_TOO_MANY_MEMBERS:
            case CANNOT_SEND_VIDEO:
            case PLAYBACK_FAILURE:
                ExceptionHandler.saveException(new RuntimeException("Unhandled error " + error.getType()), null);
                break;
            case CANNOT_SEND_MESSAGE_TO_UNVERIFIED_CONVERSATION:
            case RECORDING_FAILURE:
            case CANNOT_SEND_ASSET_FILE_NOT_FOUND:
            case CANNOT_SEND_ASSET_TOO_LARGE:
                // Handled in ConversationFragment
                break;
            default:
                ExceptionHandler.saveException(new RuntimeException("Unexpected error " + error.getType()), null);

        }
    }

    @Override
    public void onDialogConfirm(String dialogId) {
        getChildFragmentManager().popBackStackImmediate(ConfirmationFragment.TAG,
                                                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
        dismissError(dialogId);
    }

    @Override
    public void onDialogCancel(String dialogId) {
        getChildFragmentManager().popBackStackImmediate(ConfirmationFragment.TAG,
                                                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
        dismissError(dialogId);
    }

    private void dismissError(String errorId) {
        if (getActivity() == null ||
            getStoreFactory().isTornDown()) {
            return;
        }
        getStoreFactory().getInAppNotificationStore().dismissError(errorId);
    }

    public interface Container {
        void onOpenUrl(String url);
    }
}
