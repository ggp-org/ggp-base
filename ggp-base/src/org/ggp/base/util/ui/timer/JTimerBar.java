package org.ggp.base.util.ui.timer;

import javax.swing.JProgressBar;

@SuppressWarnings("serial")
public final class JTimerBar extends JProgressBar
{

	private final class TimerThread extends Thread
	{

		private final long delta;
		private long time;
		private final long timeout;

		public TimerThread(long delta, long timeout)
		{
			this.delta = delta;
			this.timeout = timeout;
			time = 0;
		}

		@Override
		public synchronized void run()
		{
			try
			{
				while (time != timeout)
				{
					time += delta;
					wait(delta);
					setValue((int) time);
				}
			}
			catch (InterruptedException e)
			{
				// Do nothing.
			}
		}
	}

	private TimerThread timerThread;

	public JTimerBar()
	{
		timerThread = null;
	}

	public synchronized void fill()
	{
		stop();
		this.setValue(getMaximum());
	}

	public synchronized void stop()
	{
		try
		{
			if (timerThread != null)
			{
				timerThread.interrupt();
				timerThread.join();
			}

			setValue(0);
		}
		catch (Exception e)
		{
			setIndeterminate(true);
		}
	}

	public synchronized void time(long time, int divisions)
	{
		try
		{
			stop();
			setMaximum((int) time);

			timerThread = new TimerThread(time / divisions, time);
			timerThread.start();
		}
		catch (Exception e)
		{
			setIndeterminate(true);
		}
	}
}