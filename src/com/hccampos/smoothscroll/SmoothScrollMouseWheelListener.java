package com.hccampos.smoothscroll;

import com.intellij.openapi.editor.ScrollingModel;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

class SmoothScrollMouseWheelListener implements MouseWheelListener, ActionListener {
    private static final int FPS = 50;
    private static final int MILLIS_PER_FRAME = 1000 / FPS;

    private static final double STEP = 0.0004;
    private static final double MAX_FORCE = 0.08;
    private static final double PSEUDO_FRICTION = 0.93;
    private static final double ACC_REDUCTION_FACTOR = 0.8;
    private static final double SPEED_THRESHOLD = 0.001;

    private ScrollingModel _scrollingModel;
    private double _velocity = 0.0;
    private double _force = 0.0;
    private long _lastUpdate = 0;
    private long _lastScroll = 0;

    private Timer _timer = null;

    /**
     * Constructor for our MouseWheelListener.
     *
     * @param editor
     *      The file editor to which smooth scrolling is to be added.
     */
    public SmoothScrollMouseWheelListener(FileEditor editor) {
        _scrollingModel = ((TextEditor) editor).getEditor().getScrollingModel();
        _scrollingModel.disableAnimation();
        _timer = new Timer(MILLIS_PER_FRAME, this);
    }

    /**
     * Starts animating the scroll offset.
     */
    public void startAnimating() {
        _timer.start();
    }

    /**
     * Stops animating the scroll offset.
     */
    public void stopAnimating() {
        _timer.stop();
    }

    /**
     * Updates the velocity, force and acceleration acting on the scroll offset and then updates
     * the scroll offset according to these parameters.
     */
    protected void update() {
        if (_lastUpdate == 0) {
            _lastUpdate = System.nanoTime();
            return;
        }

        long currentTime = System.nanoTime();
        double elapsedMillis = (currentTime - _lastUpdate) * 1.0e-6;
        _lastUpdate = currentTime;

        double exponent = elapsedMillis / 16.0;

        _force *= Math.pow(ACC_REDUCTION_FACTOR, exponent);
        _velocity += _force * elapsedMillis;
        _velocity *= Math.pow(PSEUDO_FRICTION, exponent);

        double speed = Math.abs(_velocity);
        if (speed >= SPEED_THRESHOLD) {
            int currentOffset = _scrollingModel.getVerticalScrollOffset();
            _scrollingModel.scrollVertically(Math.max(0, (int)Math.round(currentOffset + _velocity * elapsedMillis)));
        } else {
            _velocity = 0.0;
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
        double elapsedMillis = (currentTime - _lastScroll) * 1.0e-6;
        _lastScroll = currentTime;

        if (elapsedMillis == 0) { return; }

        double wheelDelta = e.getPreciseWheelRotation();
        boolean sameDirection = _velocity * wheelDelta >= 0;

        if (sameDirection) {
            double currentStep = wheelDelta * STEP;
            _force += currentStep + currentStep / (elapsedMillis * 0.0007);

            // Limit the magnitude of the force to MAX_FORCE.
            double forceMagnitude = Math.abs(_force);
            if (forceMagnitude > MAX_FORCE) { _force *= MAX_FORCE / forceMagnitude; }
        } else {
            _force = 0;
            _velocity = 0;
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

    @Override
    public void actionPerformed(ActionEvent e) {
        update();
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
