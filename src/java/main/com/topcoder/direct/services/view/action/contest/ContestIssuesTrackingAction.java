/*
 * Copyright (C) 2011 TopCoder Inc., All Rights Reserved.
 */
package com.topcoder.direct.services.view.action.contest;

import com.topcoder.direct.services.configs.ConfigUtils;
import com.topcoder.direct.services.view.action.contest.launch.DirectStrutsActionsHelper;
import com.topcoder.direct.services.view.action.contest.launch.StudioOrSoftwareContestAction;
import com.topcoder.direct.services.view.dto.TcJiraIssue;
import com.topcoder.direct.services.view.dto.UserProjectsDTO;
import com.topcoder.direct.services.view.dto.contest.ContestIssuesTrackingDTO;
import com.topcoder.direct.services.view.dto.contest.ContestStatsDTO;
import com.topcoder.direct.services.view.dto.contest.TypedContestBriefDTO;
import com.topcoder.direct.services.view.dto.project.ProjectBriefDTO;
import com.topcoder.direct.services.view.util.DataProvider;
import com.topcoder.direct.services.view.util.DirectUtils;
import com.topcoder.direct.services.view.util.SessionData;
import com.topcoder.direct.services.view.util.jira.JiraRpcServiceWrapper;
import com.topcoder.security.TCSubject;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Action class which handles retrieving Jira issues and bug races for the contest</p>
 *
 * @author TCSDEVELOPER
 * @version 1.0
 */
public class ContestIssuesTrackingAction extends StudioOrSoftwareContestAction {

    /**
     * <p>A <code>SessionData</code> providing interface to current session.</p>
     */
    private SessionData sessionData;

    /**
     * <p>A <code>ContestIssuesTrackingDTO</code> providing the view data for displaying by
     * <code>Contest issues and bug races</code> view. </p>
     */
    private ContestIssuesTrackingDTO viewData;

    /**
     * Gets the view data for this action.
     *
     * @return the view data for this action.
     */
    public ContestIssuesTrackingDTO getViewData() {
        return viewData;
    }

    /**
     * Initialize the action. The constructor will initialize an empty view data.
     */
    public ContestIssuesTrackingAction() {
        this.viewData = new ContestIssuesTrackingDTO();
    }

    /**
     * <p>Handles the incoming request. If action is executed successfully then changes the current project context to
     * project for contest requested for this action.</p>
     *
     * @throws Exception if an unexpected error occurs while processing the request.
     */
    @Override
    public void executeAction() throws Exception {
        // Get current session
        HttpServletRequest request = DirectUtils.getServletRequest();
        this.sessionData = new SessionData(request.getSession());

        TCSubject currentUser = DirectStrutsActionsHelper.getTCSubjectFromSession();

        // Set registrants data
        long contestId;
        if (isStudioCompetition()) {
            contestId = getContestId();
        } else {
            contestId = getProjectId();
        }

        // get issues and bug races from the Jira RPC soap service
        List<TcJiraIssue> results = JiraRpcServiceWrapper.getIssuesForContest(contestId, isStudioCompetition());

        // use one list to store issues, another list to store bug races
        List<TcJiraIssue> issues = new ArrayList<TcJiraIssue>();
        List<TcJiraIssue> bugRaces = new ArrayList<TcJiraIssue>();

        // get the jira project name for bug race from the configuration. It will be used to tell which issue
        // is a bug race
        String bugRaceProjectName = ConfigUtils.getIssueTrackingConfig().getBugRaceProjectName().trim().toLowerCase();


        // filter out the jira issues and bug races
        for (TcJiraIssue item : results) {
            if(item.getProjectName().trim().toLowerCase().equals(bugRaceProjectName)) {
                bugRaces.add(item);
            } else {
                issues.add(item);
            }
        }


        // set the view data
        getViewData().setContestId(contestId);
        getViewData().setIssues(issues);
        getViewData().setBugRaces(bugRaces);


        // Set contest stats
        ContestStatsDTO contestStats = DirectUtils.getContestStats(currentUser, contestId, isStudioCompetition());
        getViewData().setContestStats(contestStats);

        // Set projects data
        List<ProjectBriefDTO> projects = DataProvider.getUserProjects(currentUser.getUserId());
        UserProjectsDTO userProjectsDTO = new UserProjectsDTO();
        userProjectsDTO.setProjects(projects);
        getViewData().setUserProjects(userProjectsDTO);

        // Set current project contests
        List<TypedContestBriefDTO> contests = DataProvider.getProjectTypedContests(
                currentUser.getUserId(), contestStats.getContest().getProject().getId());
        this.sessionData.setCurrentProjectContests(contests);

        // Set current project context based on selected contest
        this.sessionData.setCurrentProjectContext(contestStats.getContest().getProject());
        this.sessionData.setCurrentSelectDirectProjectID(contestStats.getContest().getProject().getId());
    }


    /**
     * <p>Gets the current session associated with the incoming request from client.</p>
     *
     * @return a <code>SessionData</code> providing access to current session.
     */
    public SessionData getSessionData() {
        return this.sessionData;
    }

}