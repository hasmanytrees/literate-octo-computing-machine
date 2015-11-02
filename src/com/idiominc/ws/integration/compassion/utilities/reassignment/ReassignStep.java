package com.idiominc.ws.integration.compassion.utilities.reassignment;

//sdk
import com.idiominc.wssdk.workflow.*;
import com.idiominc.wssdk.user.WSRole;
import com.idiominc.wssdk.user.WSUser;
import com.idiominc.wssdk.user.WSLocale;
import com.idiominc.wssdk.user.WSWorkgroup;

/**
 * Helper class to check user availability and reassign steps
 */
public class ReassignStep {

    /**
     * Check if step is assigned to role
     * @param hts - human workflow step
     * @param role - workflow role
     * @return true, if step is assigned to passed role
     */
    public static boolean belongs(WSHumanTaskStep hts, WSRole role) {
         WSRole[] roles = hts.getRoleAssignees();
         if(belongs(role, roles)) {
                 return true;
         }
         WSUser[] users = hts.getUserAssignees();
         if(users != null && users.length > 0) {
           for(WSUser u: users) {
               roles = u.getRoles();
               if(!belongs(role, roles)) {
                     return false;
               }
           }
           return true;
         }
         return false;
     }

    /**
     * Check if role is in the list of passed roles
     * @param targetRole - role to check
     * @param roles - list of roles
     * @return true if passed role is found in the list
     */
     public static boolean belongs(WSRole targetRole, WSRole[] roles) {
         if(null == roles || roles.length == 0) return false;
         for(WSRole r: roles) {
           if(r.getId() == targetRole.getId()) {
             return true;
           }
         }
         return false;
     }

    /**
     * Check if user belongs to the locale's user
     * @param userID - user ID
     * @param locale - WorldServer locale
     * @return true if user belongs to the locale's users
     */
    public static boolean belongs(int userID, WSLocale locale) {
        WSUser[] lcUsers = locale.getUsers();
        if(null == lcUsers || lcUsers.length == 0) {
            return false;
        }
        for(WSUser u: lcUsers) {
            if(u.getId() == userID) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user belongs to the role's user
     * @param userID - user ID
     * @param role - WorldServer role
     * @return true if user belongs to the role's users
     */
    public static boolean belongs(int userID, WSRole role) {
        WSUser[] rlUsers = role.getUsers();
        if(null == rlUsers || rlUsers.length == 0) {
            return false;
        }
        for(WSUser u: rlUsers) {
            if(u.getId() == userID) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user belongs to the workgroup's user
     * @param userID - user ID
     * @param workgroup - WorldServer workgroup
     * @return true if user belongs to the workgroup's users
     */

    public static boolean belongs(int userID, WSWorkgroup workgroup) {
        WSUser[] wgUsers = workgroup.getUsers();
        if(null == wgUsers || wgUsers.length == 0) {
            return false;
        }
        for(WSUser u: wgUsers) {
            if(u.getId() == userID) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reassign step to the list of users
     * @param step - step to reassign
     * @param users - users to whom step should be assigned to
     */
    public static void reassign(WSHumanTaskStep step, WSUser[] users) {
       step.setUserAssignees(users);
       step.setRoleAssignees(new WSRole[0]);
    }

    /**
     * Obtain user who has completed last human task step
     * @param task - worldserver task
     * @return User, or nulkl if not found
     */
    public static WSUser getPrevousTaskStepExecutor(WSTask task) {
     /*
        WSTaskStep current = task.getCurrentTaskStep();
        WSTaskStep previous = current.getPreviousTaskStep();
        while(previous != null && !(previous instanceof WSHumanTaskStep)) {
            previous = previous.getPreviousTaskStep();
        }
        if(null == previous) {
            return null;
        }
        WSHumanTaskStep htStep = (WSHumanTaskStep) previous;
        //step.getCurrentClaimant() - likely will not work!!
        WSTaskStepHistory history = StepCompletionInformation.getStepHistoryFromTaskStep(task, htStep);
        if(null == history) {
            return null;
        }
        return history.getUser();
      */
      WSTaskHistory history = task.getTaskHistory();
      if(null == history) {
          return null;
      }
      return history.getLastHumanStepUser();
    }

    /**
     * Obtain the very next human step in the workflow
      * @param task - WorldServer's task object
     *  @return Next human task step, or null if not found
     */
    public static WSHumanTaskStep getNextHumanTaskStep(WSTask task) {
        WSTaskStep current = task.getCurrentTaskStep();
        WSTaskStep next = current.getNextTaskStep(current.getWorkflowStep().getTransitions()[0]);
        while(next != null && !(next instanceof WSHumanTaskStep)) {
            next = next.getNextTaskStep(next.getWorkflowStep().getTransitions()[0]);
        }
        if(null == next) {
            return null;
        }
        return (WSHumanTaskStep) next;

    }

}
