package com.aitranslator.app.utils;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

public class AnimUtils {
    public static void fadeInUp(View view, long delay) {
        view.setAlpha(0f); view.setTranslationY(40f);
        view.animate().alpha(1f).translationY(0f).setStartDelay(delay)
                .setDuration(450).setInterpolator(new DecelerateInterpolator(1.5f)).start();
    }

    public static void fadeInScale(View view, long delay) {
        view.setAlpha(0f); view.setScaleX(0.85f); view.setScaleY(0.85f);
        view.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(delay)
                .setDuration(500).setInterpolator(new OvershootInterpolator(1.2f)).start();
    }

    public static void slideInFromLeft(View view, long delay) {
        view.setAlpha(0f); view.setTranslationX(-60f);
        view.animate().alpha(1f).translationX(0f).setStartDelay(delay)
                .setDuration(400).setInterpolator(new DecelerateInterpolator(1.5f)).start();
    }

    public static void slideInFromRight(View view, long delay) {
        view.setAlpha(0f); view.setTranslationX(60f);
        view.animate().alpha(1f).translationX(0f).setStartDelay(delay)
                .setDuration(400).setInterpolator(new DecelerateInterpolator(1.5f)).start();
    }

    public static void popIn(View view) {
        view.setScaleX(0f); view.setScaleY(0f); view.setAlpha(0f); view.setVisibility(View.VISIBLE);
        view.animate().scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(350).setInterpolator(new OvershootInterpolator(1.5f)).start();
    }

    public static void pulse(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.08f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.08f, 1f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(400);
        set.start();
    }

    public static void startTypingAnimation(View dot1, View dot2, View dot3) {
        long base = 0;
        for (View dot : new View[]{dot1, dot2, dot3}) {
            final long delay = base;
            dot.animate().translationY(-8f).setStartDelay(delay).setDuration(300)
                    .withEndAction(() -> dot.animate().translationY(0f).setDuration(300)
                            .setInterpolator(new DecelerateInterpolator()).start())
                    .setInterpolator(new DecelerateInterpolator()).start();
            base += 150;
        }
    }
}