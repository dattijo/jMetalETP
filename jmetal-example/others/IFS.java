/** Solver thread */
protected class SolverThread extends Thread {

    /** Solving rutine */
    @Override
    public void run() {
		try {
			iStop = false;
			// Sets thread name
			setName("Solver");
			
			// Initialization
			iProgress = Progress.getInstance(iCurrentSolution.getModel());
			iProgress.setStatus("Solving problem ...");
			iProgress.setPhase("Initializing solver");
			initSolver();
			onStart();

			double startTime = JProf.currentTimeSec();
			if (isUpdateProgress()) {
				if (iCurrentSolution.getBestInfo() == null) {
					iProgress.setPhase("Searching for initial solution ...", iCurrentSolution.getModel().variables().size());
				} else {
					iProgress.setPhase("Improving found solution ...");
				}
			}

			long prog = 9999;
			sLogger.info("Initial solution:" + ToolBox.dict2string(iCurrentSolution.getInfo(), 1));
			if ((iSaveBestUnassigned < 0 || iSaveBestUnassigned >= iCurrentSolution.getModel().nrUnassignedVariables())
				&& 
				(iCurrentSolution.getBestInfo() == null || getSolutionComparator().isBetterThanBestSolution(iCurrentSolution))){
				if (iCurrentSolution.getModel().nrUnassignedVariables() == 0)
					sLogger.info("Complete solution " + ToolBox.dict2string(iCurrentSolution.getInfo(), 1) + " was found.");
					synchronized (iCurrentSolution) {
					iCurrentSolution.saveBest();
				}
			}

			if (iCurrentSolution.getModel().variables().isEmpty()) {
				iProgress.error("Nothing to solve.");
				iStop = true;
			}

			// Iterations: until solver can continue
			while (!iStop && getTerminationCondition().canContinue(iCurrentSolution)) {
				// Neighbour selection
				Neighbour<V, T> neighbour = getNeighbourSelection().selectNeighbour(iCurrentSolution);
				for (SolverListener<V, T> listener : iSolverListeners) {
					if (!listener.neighbourSelected(iCurrentSolution.getIteration(), neighbour)) {
						neighbour = null;
						continue;
					}
				}
				if (neighbour == null) {
					sLogger.debug("No neighbour selected.");
					synchronized (iCurrentSolution) { // still update the solution (increase iteration etc.)
						iCurrentSolution.update(JProf.currentTimeSec() - startTime);
					}
					continue;
				}

				// Assign selected value to the selected variable
				synchronized (iCurrentSolution) {
					neighbour.assign(iCurrentSolution.getIteration());
					iCurrentSolution.update(JProf.currentTimeSec() - startTime);
				}

				onAssigned(startTime);

				// Check if the solution is the best ever found one
				if ((iSaveBestUnassigned < 0 || iSaveBestUnassigned >= iCurrentSolution.getModel().nrUnassignedVariables())
					&& (iCurrentSolution.getBestInfo() == null || getSolutionComparator().isBetterThanBestSolution(iCurrentSolution))) {
					if (iCurrentSolution.getModel().nrUnassignedVariables() == 0) {
						iProgress.debug("Complete solution of value " + iCurrentSolution.getModel().getTotalValue()+ " was found.");
					}
					synchronized (iCurrentSolution) {
						iCurrentSolution.saveBest();
					}
				}

				// Increment progress bar
				if (isUpdateProgress()) {
					if (iCurrentSolution.getBestInfo() != null && iCurrentSolution.getModel().getBestUnassignedVariables() == 0) {
						prog++;
						if (prog == 10000) {
							iProgress.setPhase("Improving found solution ...");
							prog = 0;
						} else {
							iProgress.setProgress(prog / 100);
						}
					} else if ((iCurrentSolution.getBestInfo() == null || iCurrentSolution.getModel().getBestUnassignedVariables() > 0)
							&& (iCurrentSolution.getModel().variables().size() - iCurrentSolution.getModel().nrUnassignedVariables()) > iProgress.getProgress()) {
							iProgress.setProgress(iCurrentSolution.getModel().variables().size()- iCurrentSolution.getModel().nrUnassignedVariables());
					}
				}
			}

				// Finalization
				iLastSolution = iCurrentSolution;

				iProgress.setPhase("Done", 1);
				iProgress.incProgress();

				iSolverThread = null;
				if (iStop) {
					sLogger.debug("Solver stopped.");
					iProgress.setStatus("Solver stopped.");
					onStop();
				} else {
					sLogger.debug("Solver done.");
					iProgress.setStatus("Solver done.");
					onFinish();
				}
		} catch (Exception ex) {
			sLogger.error(ex.getMessage(), ex);
			iProgress.fatal("Solver failed, reason:" + ex.getMessage(), ex);
			iProgress.setStatus("Solver failed.");
			onFailure();
		}
		iSolverThread = null;
	}
}
