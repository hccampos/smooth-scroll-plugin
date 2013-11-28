package com.hccampos.smoothscroll;

import com.intellij.openapi.editor.ScrollingModel;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;

import javax.swing.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

class SmoothScrollMouseWheelListener implements MouseWheelListener {
    private static final double STEP = 0.08;
    private static final double MAX_SPEED = 30.0;
    private static final double MAX_FORCE = 20.0;
    private static final double FORCE_REDUCTION_FACTOR = 0.6;
    private static final double PSEUDO_FRICTION = 0.95;
    private static final double GLOBAL_SCALE_FACTOR = 0.1;
    private static final double VELOCITY_SCALE_FACTOR = 0.7f;

    private ScrollingModel _scrollingModel;
    private long _lastTime = 0;
    private double _vel = 0.0;
    private double _force = 0.0;
    private boolean _animating = false;

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
                        Thread.sleep(20);
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
        if (_lastTime == 0) {
            _lastTime = System.currentTimeMillis();
            return;
        }

        // Determine the time that has passed since the last update.
        long currentTime = System.currentTimeMillis();
        long t = currentTime - _lastTime;
        _lastTime = currentTime;

        // Update the force and then update the velocity according to Newtown -> F=ma
        _force *= FORCE_REDUCTION_FACTOR;
        _vel = _vel * PSEUDO_FRICTION + _force * t; // F = ma, but since m = 1, F = a

        // Make sure the speed limit is not exceeded.
        _vel = limitMagnitude(_vel, MAX_SPEED);

        // Here we scale the velocity and then square it. This way, the scroll offset will not
        // be moved linearly according to the velocity.
        double scaledVel = _vel * VELOCITY_SCALE_FACTOR;
        double delta = scaledVel * Math.abs(scaledVel) * t * GLOBAL_SCALE_FACTOR;

        if (Math.abs(delta) >= 1.0f) {
            SwingUtilities.invokeLater(new UpdateScrollRunnable((int) delta));
        }
    }

    /**
     * Function which is called when the mouse wheel is rotated.
     *
     * @param e
     *      The object which contains the information of the event.
     */
    public void mouseWheelMoved(MouseWheelEvent e) {
        double wheelDelta = e.getPreciseWheelRotation();

        // If we're scrolling in the same direction, we can increase the force.
        // Otherwise, the user wants to go the other way right away, so we reset both the
        // force and the velocity.
        boolean sameDirection = wheelDelta * _vel >= 0;
        if (sameDirection) {
            _force += wheelDelta * STEP;
        } else {
            _force = 0;
            _vel = 0;
        }

        // Make sure the force does not exceed MAX_FORCE.
        _force = limitMagnitude(_force, MAX_FORCE);
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
        private int _delta;

        public UpdateScrollRunnable(int delta) {
            _delta = delta;
        }

        public void run() {
            int currentOffset = _scrollingModel.getVerticalScrollOffset();
            _scrollingModel.scrollVertically(Math.max(0, currentOffset + _delta));
        }
    }
}
