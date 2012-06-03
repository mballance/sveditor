package net.sf.sveditor.core.job_mgr;

import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.job_mgr.JobMgrWorkerThread.ThreadState;

public class JobMgr implements IJobMgr {
	
	private List<IJobListener>			fJobListeners;
	private List<JobMgrWorkerThread>	fThreadPool;
	private List<IJob>					fJobQueue;
	private int						fMaxThreads;
	private boolean					fDisposed;
	
	public JobMgr() {
		fJobListeners = new ArrayList<IJobListener>();
		fThreadPool = new ArrayList<JobMgrWorkerThread>();
		fJobQueue = new ArrayList<IJob>();
		
		fMaxThreads = 4;
	}
	
	public void dispose() {
		fDisposed = true;
		
		// Wait for all the threads to exit
		synchronized (fThreadPool) {
			while (fThreadPool.size() > 0) {
				try {
					fThreadPool.wait();
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}
	
	public void addJobListener(IJobListener l) {
		synchronized (fJobListeners) {
			fJobListeners.add(l);
		}
	}

	public void removeJobListener(IJobListener l) {
		synchronized (fJobListeners) {
			fJobListeners.remove(l);
		}
	}
	
	public IJob createJob() {
		return new JobMgrJob();
	}

	public void queueJob(IJob job) {
		checkWorkerThreads();
		synchronized (fJobQueue) {
			fJobQueue.add(job);
			fJobQueue.notifyAll();
		}
	}

	/**
	 * Check to see if we should launch a new thread
	 */
	private void checkWorkerThreads() {
		synchronized (fThreadPool) {
			boolean all_busy = true;
			for (JobMgrWorkerThread t : fThreadPool) {
				if (t.getThreadState() == ThreadState.Waiting) {
					all_busy = false;
				}
			}
			if (all_busy && fThreadPool.size() < fMaxThreads) {
				JobMgrWorkerThread t = new JobMgrWorkerThread(this);
				fThreadPool.add(t);
				t.start();
			}
		}
	}
	
	public IJob dequeueJob(int idle_timeout) {
		IJob job = null;
		for (int i=0; i<2; i++) {
			synchronized (fJobQueue) {
				if (fJobQueue.size() > 0) {
					job = fJobQueue.remove(0);
				} else if (i == 0) {
					// Wait for a bit
					try {
						fJobQueue.wait(idle_timeout);
					} catch (InterruptedException e) {}
				}
			}
		}
		
		if (job != null) {
			jobStarted(job);
		}
		
		return job;
	}
	
	private void jobStarted(IJob job) {
		synchronized (fJobListeners) {
			for (IJobListener l : fJobListeners) {
				l.jobStarted(job);
			}
		}
	}

	void jobEnded(IJob job) {
		synchronized (fJobListeners) {
			for (IJobListener l : fJobListeners) {
				l.jobEnded(job);
			}
		}
	}

	/**
	 * Called by the worker thread to see if it can exit
	 * 
	 * @param t
	 * @return
	 */
	public boolean tryToExit(JobMgrWorkerThread t) {
		boolean can_exit = true;
		synchronized (fThreadPool) {
			can_exit = (fThreadPool.size() > 1 || fDisposed);
			if (can_exit) {
				fThreadPool.remove(t);
				fThreadPool.notifyAll();
			}
		}
		return can_exit;
	}
}
