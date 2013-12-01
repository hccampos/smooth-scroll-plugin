package com.hccampos.smoothscroll;

import com.intellij.openapi.editor.ScrollingModel;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;

import javax.swing.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

class SmoothScrollMouseWheelListener implements MouseWheelListener {
    private static final int FPS = 50;
    private static final int MILLIS_PER_FRAME = 1000 / FPS;

    private static final double STEP = 0.025 * MILLIS_PER_FRAME;
    private static final double MAX_SPEED = 3.0 * MILLIS_PER_FRAME;
    private static final double MAX_ACCELERATION = 2.5 * MILLIS_PER_FRAME;
    private static final double PSEUDO_FRICTION = Math.pow(0.90, MILLIS_PER_FRAME / 16.0);
    private static final double ACC_REDUCTION_FACTOR = Math.pow(0.8, MILLIS_PER_FRAME / 16.0);

    private ScrollingModel _scrollingModel;
    private double _vel = 0.0;
    private double _acc = 0.0;
    private boolean _animating = false;
    private long _lastScroll = 0;

    /**
     * Constructor for our MouseWheelListener.
     *
     * @param editor
     *      The file editor to which smooth scrolling is to be added.
     */
    public SmoothScrollMouseWheelListener(FileEditor editor) {
        _scrollingModel = ((TextEditor) editor).getEditor().getScrollingModel();
        _scrollingModel.disableAnimation();
    }

    /**
     * Starts animating the scroll offset.
     */
    public void startAnimating() {
        if (_animating) { return; }

        Thread _animationThread = new Thread() {
            public void run() {
                while (_animating) {
                    update();
                    try {
                        Thread.sleep(MILLIS_PER_FRAME);
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        };

        _animationThread.start();
        _animating = true;
    }

    /**
     * Stops animating the scroll offset.
     */
    public void stopAnimating() {
        _animating = false;
    }

    /**
     * Updates the velocity, force and acceleration acting on the scroll offset and then updates
     * the scroll offset according to these parameters.
     */
    protected void update() {
        _acc *= ACC_REDUCTION_FACTOR;
        _vel += _acc;
        _vel *= PSEUDO_FRICTION;
        _vel = limitMagnitude(_vel, MAX_SPEED);

        if (Math.abs(_vel) >= 1.0) {
            SwingUtilities.invokeLater(new UpdateScrollRunnable(_vel));
        }
    }

    /**
     * Function which is called when the mouse wheel is rotated.
     *
     * @param e
     *      The object which contains the information of the event.
     */
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (_lastScroll == 0) {
            _lastScroll = System.nanoTime();
            return;
        }

        long currentTime = System.nanoTime();
        double elapsedTime = (currentTime - _lastScroll) * 1.0e-9;
        _lastScroll = currentTime;

        if (elapsedTime == 0)
            return;

        double wheelDelta = e.getPreciseWheelRotation();
        boolean sameDirection = _vel * wheelDelta >= 0;

        if (sameDirection) {
            double currentStep = wheelDelta * STEP;
            _acc += currentStep + currentStep / elapsedTime;
            _acc = limitMagnitude(_acc, MAX_ACCELERATION);
        } else {
            _acc = 0;
            _vel = 0;
        }
    }

    /**
     * Makes sure the magnitude of the specified value does not exceed maxMagnitude. If the value
     * exceeds maxMagnitude, it is normalized and the then multiplied by maxMagnitude before being
     * returned.
     *
     * @param value
     *      The value which is to be processed.
     * @param maxMagnitude
     *      The maximum magnitude that the value should have.
     *
     * @return
     *      The specified value or, if the value exceeds maxMagnitude, a new value with the same
     *      signal but with magnitude of maxMagnitude.
     */
    private double limitMagnitude(double value, double maxMagnitude) {
        double magnitude = Math.abs(value);
        if (magnitude > maxMagnitude) {
            return value / magnitude * maxMagnitude;
        } else {
            return value;
        }
    }

    /**
     * Simple Runnable which updates the scroll offset of the editor. We have to do this here
     * because the scroll offset can only be read and set from the event dispatching thread.
     */
    private class UpdateScrollRunnable implements Runnable {
        private double _delta;

        public UpdateScrollRunnable(double delta) {
            _delta = delta;
        }

        public void run() {
            int currentOffset = _scrollingModel.getVerticalScrollOffset();
            _scrollingModel.scrollVertically(Math.max(0, (int)Math.round(currentOffset + _delta)));
        }
    }
}
