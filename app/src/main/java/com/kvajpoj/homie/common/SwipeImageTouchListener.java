package com.kvajpoj.homie.common;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

import org.apache.log4j.Logger;

public class SwipeImageTouchListener implements View.OnTouchListener{

    private final View swipeView;
    private final ISwipeImageCallback mSwipeImageCallback;
    private boolean isLeftEnabled = false;
    private boolean isRightEnabled = false;
    private boolean isUpEnabled = true;
    private boolean isDownEnabled = false;
    private int position = 0;

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public boolean GetLeftEnabled() { return isLeftEnabled; }
    public void SetLeftEnabled(boolean leftEnabled) { isLeftEnabled = leftEnabled; }

    public boolean GetRightEnabled() { return isRightEnabled; }
    public void SetRightEnabled(boolean rightEnabled) { isRightEnabled = rightEnabled; }

    public boolean GetUpEnabled() { return isUpEnabled; }
    public void SetUpEnabled(boolean upEnabled) { isUpEnabled = upEnabled; }

    public boolean GetDownEnabled() { return isDownEnabled; }
    public void SetDownEnabled(boolean downEnabled) { isDownEnabled = downEnabled; }

    private Logger LOG = Logger.getLogger(SwipeImageTouchListener.class);

    public interface ISwipeImageCallback {
        void onSwipeImageUp(SwipeImageTouchListener sender, View swipeView);
        void onSwipeImageDown(SwipeImageTouchListener sender, View swipeView);
        void onSwipeImageLeft(SwipeImageTouchListener sender, View swipeView);
        void onSwipeImageRight(SwipeImageTouchListener sender, View swipeView);
    }


    public SwipeImageTouchListener(View swipeView, ISwipeImageCallback cb) {
        this.swipeView = swipeView;
        this.mSwipeImageCallback = cb;
    }

    // Allows us to know if we should use MotionEvent.ACTION_MOVE
    private boolean tracking = false;
    // The Position where our touch event started
    private float startY;
    private float startX;



    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Rect hitRect = new Rect();
                swipeView.getHitRect(hitRect);
                if (hitRect.contains((int) event.getX(), (int) event.getY())) {
                    tracking = true;
                }
                startY = event.getY();
                startX = event.getX();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                tracking = false;
                if((swipeView.getX() > 50 && !GetRightEnabled()) ||
                   (swipeView.getX() < -50 && !GetLeftEnabled()) ||
                   (swipeView.getY() < -50 && !GetUpEnabled())   ||
                   (swipeView.getY() > 50 && !GetDownEnabled()))
                {
                    swipeView.setTranslationY(0);
                    swipeView.setTranslationX(0);
                    return true;
                }
                animateSwipeView(v.getHeight(), v.getWidth());
                return true;
            case MotionEvent.ACTION_MOVE:
                if (tracking) {
                    //LOG.info("Move to X:" + Math.abs( event.getX() - startX ) + " - Move to Y " + Math.abs(event.getY() - startY) + " " + swipeView.getX() + " " + swipeView.getY());
                    //determine direction
                    if((swipeView.getX() > 50 && !GetRightEnabled()) ||
                        (swipeView.getX() < -50 && !GetLeftEnabled()) ||
                        (swipeView.getY() < -50 && !GetUpEnabled())   ||
                        (swipeView.getY() > 50 && !GetDownEnabled()))
                    {
                        return true;
                    }
                    // move to Y (up - down)
                    if( Math.abs(event.getY() - startY) >= Math.abs( event.getX() - startX ) && Math.abs(swipeView.getX()) < 5 ) {
                        swipeView.setTranslationY(event.getY() - startY);
                        swipeView.setTranslationX(0);
                    }
                    // move to X (left - right)
                    else if ( Math.abs( event.getX() - startX) >= Math.abs(event.getY() - startY) && Math.abs(swipeView.getY()) < 5 ) {
                        swipeView.setTranslationX(event.getX() - startX);
                        swipeView.setTranslationY(0);
                    }
                }
                return true;
        }
        return false;
    }

    /**
     * Using the current translation of swipeView decide if it has moved
     * to the point where we want to remove it.
     */
    private void animateSwipeView(int parentHeight, int parentWidth) {
        int quarterHeight = parentHeight / 4;
        int quarterWidth = parentWidth / 4;
        float currentPositionY = swipeView.getTranslationY();
        float currentPositionX = swipeView.getTranslationX();
        float animateTo = 0.0f;
        if (currentPositionY < -quarterHeight) {
            animateTo = -parentHeight;
            animateToDismiss(currentPositionY, animateTo);
        }
        else if (currentPositionY > quarterHeight) {
            animateTo = parentHeight;
            animateToDismiss(currentPositionY, animateTo);
        }
        else if (currentPositionX > quarterWidth) {
            animateTo = parentWidth;
            animateToLoad(currentPositionX, animateTo);
        }
        else if (currentPositionX < -quarterWidth) {
            animateTo = -parentWidth;
            animateToLoad(currentPositionX, animateTo);
        }
        else {
            ObjectAnimator oaY = ObjectAnimator.ofFloat(swipeView, "translationY", currentPositionY, animateTo);
            oaY.setDuration(200);
            oaY.start();

            ObjectAnimator oaX = ObjectAnimator.ofFloat(swipeView, "translationX", currentPositionX, animateTo);
            oaX.setDuration(200);
            oaX.start();
        }
    }

    private void animateToDismiss(final float currentPosition, final float animateTo){
        ObjectAnimator oa = ObjectAnimator.ofFloat(swipeView, "translationY", currentPosition, animateTo);
        oa.setDuration(150);
        final SwipeImageTouchListener self = this;

        oa.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if(mSwipeImageCallback != null) {
                    if(currentPosition > animateTo)
                        mSwipeImageCallback.onSwipeImageUp(self, swipeView);
                    else
                        mSwipeImageCallback.onSwipeImageDown(self, swipeView);
                }
                //if(mDialog != null)
                //    mDialog.dismiss();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        oa.start();
    }

    private void animateToLoad(final float currentPosition, final float animateTo){
        final SwipeImageTouchListener self = this;
        ObjectAnimator oa = ObjectAnimator.ofFloat(swipeView, "translationX", animateTo, currentPosition);
        oa.setDuration(150);
        oa.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                swipeView.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if(mSwipeImageCallback != null) {
                    if(currentPosition > animateTo)
                        mSwipeImageCallback.onSwipeImageLeft(self, swipeView);
                    else
                        mSwipeImageCallback.onSwipeImageRight(self, swipeView);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        oa.start();
    }
}