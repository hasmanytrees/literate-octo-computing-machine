package com.idiominc.ws.integration.compassion.autoaction;

import com.idiominc.external.config.Config;
import com.idiominc.ws.integration.compassion.restService.KV;
import com.idiominc.ws.integration.compassion.restService.RESTException;
import com.idiominc.ws.integration.compassion.restService.RESTService;
import com.idiominc.ws.integration.profserv.commons.wssdk.autoaction.WSCustomTaskAutomaticActionWithParameters;
import com.idiominc.ws.integration.profserv.commons.wssdk.exceptions.WSInvalidParameterException;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.WSContextManager;
import com.idiominc.wssdk.WSRunnable;
import com.idiominc.wssdk.WSRuntimeException;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.component.WSParameter;
import com.idiominc.wssdk.component.WSParameterFactory;
import com.idiominc.wssdk.component.autoaction.WSActionResult;
import com.idiominc.wssdk.workflow.WSProject;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by bslack on 10/2/15.
 */
public class SetESBStatus extends WSCustomTaskAutomaticActionWithParameters {

    private static final Logger log = Logger.getLogger(SetESBStatus.class);

    private static final String _TRANSITION_DONE = "Done";

    private String statusField, statusValue = null;
    private boolean returnKit = false;
    private boolean returnError = false;

    private static final String _ATTR_STATUS_FIELD = "statusField";
    private static final String _ATTR_STATUS_VALUE = "statusValue";
    private static final String _ATTR_RETURN_ERROR = "returnError";
    private static final String _ATTR_RETURN_KIT = "includePayload";

    private static final String FO_WORKGROUP_NAME_PREFIX = "FO_";
    private static final String FO_STATUS_PREFIX = "Field Office ";
    private static final String GP_WORKGROUP_NAME_PREFIX = "GP_";
    private static final String GP_STATUS_PREFIX = "Global Partner ";

    private final static String _B2S = "Beneficiary To Supporter";

    public static void main(String[] args) {

        WSContextManager.runAsUser("admin", "wsisgreat", new WSRunnable() {

            public boolean run(WSContext context) {

                try {

                    RESTService.getInstance(context).setESBStatus(
                            context,
                            (WSAssetTask) context.getWorkflowManager().getTask(24201),
                            new SetESBStatus().generateKVs(
                                    (WSAssetTask) context.getWorkflowManager().getTask(24201),
                                    KV.parse("Status".split(";"), "Test".split(";")),
                                    false
                            ));

                } catch (RESTException e) {
                    System.out.println(e.getHttpCode());
                    System.out.println(e.getResponseText());
                } catch (Exception e) {
                    throw new WSRuntimeException(e);
                }
                return true;
            }
        });
    }

    public WSActionResult execute(WSContext context, WSAssetTask task) {
        try {

            if (returnError && returnKit) {
                throw new IOException("Illegal configuration. Cannot return Error and Kit!");
            }

            if (returnKit) {
                RESTService.getInstance(context).sendReturnKit(context,task);
            } else {
                RESTService.getInstance(context).setESBStatus(
                        context,
                        task,
                        generateKVs(
                                task,
                                KV.parse(statusField.split(";"), statusValue.split(";")),
                                returnError
                        )
                );
            }

            return new WSActionResult(_TRANSITION_DONE, "");

        } catch (IOException e) {
            log.error("ESB IO Error", e);
            return new WSActionResult(WSActionResult.ERROR, e.getMessage());
        } catch (RESTException e) {
            log.error("ESB REST API Error", e);
            return new WSActionResult(WSActionResult.ERROR, e.getMessage());
        }
    }

    private KV[] generateKVs(WSAssetTask task, KV[] kvs, boolean isError) {

        Set<KV> ret = new HashSet<KV>();
        if (kvs != null) ret.addAll(Arrays.asList(kvs));

        if (isError) {
            WSProject proj = task.getProject();
            String direction = proj.getAttribute("Direction");

            // this will over-ride any passed status
            ret.add(new KV("IsMarkedForRework", "true"));
            ret.add(new KV("ReworkComments", task.getAttribute("reworkComment")));
            if(direction != null && direction.equals(_B2S)) {
                ret.add(new KV("ReasonForRework", task.getAttribute("reworkReason")));
            } else {
                ret.add(new KV("ReasonForRework", task.getAttribute("reworkReasonS2B")));
            }
        } else {

            //TODO:Need to override the translation process/queue status to append "Field Office" or "Global Partner"
            //TODO: Move this to the payload helper class!
            //in front of translation queue / translation process status messages.
            String statusVal = "UNDEFINED";
            KV statusKV = null;
            for (KV kv : kvs) {
                if (kv.key().equals("SBCGlobalStatus")) {
                    statusVal = kv.value();
                    statusKV = kv;
                }
                break;
            }


            if(statusVal != null && !statusVal.equals("") &&
                    (statusVal.equalsIgnoreCase("translation queue") || statusVal.equalsIgnoreCase("translation process")
                    || statusVal.equalsIgnoreCase("quality check queue") || statusVal.equalsIgnoreCase("quality check process"))
                    ) {
                // need to remove existing status value; HashSet does not overwrite existing data
                if (statusKV != null && ret.contains(statusKV)) {
                    ret.remove(statusKV);
                }

                String workgroupName = task.getProject().getProjectGroup().getWorkgroup().getName();
                if (workgroupName.startsWith(FO_WORKGROUP_NAME_PREFIX))
                    ret.add(new KV("SBCGlobalStatus", FO_STATUS_PREFIX + statusVal));
                if (workgroupName.startsWith(GP_WORKGROUP_NAME_PREFIX))
                    ret.add(new KV("SBCGlobalStatus", GP_STATUS_PREFIX + statusVal));
            }
        }

        return ret.toArray(new KV[ret.size()]);

    }

    public WSParameter[] getParameters() {
        return new WSParameter[]{
                WSParameterFactory.createStringParameter(_ATTR_STATUS_FIELD, "Field(s)", ""),
                WSParameterFactory.createStringParameter(_ATTR_STATUS_VALUE, "Value(s)", ""),
                WSParameterFactory.createBooleanParameter(_ATTR_RETURN_ERROR, "Return Error?", false),
                WSParameterFactory.createBooleanParameter(_ATTR_RETURN_KIT, "Return Kit?", false)
        };
    }

    protected void preLoadParameters(Map parameters) throws WSInvalidParameterException {
        statusField = preLoadParameter(parameters, _ATTR_STATUS_FIELD, false);
        statusValue = preLoadParameter(parameters, _ATTR_STATUS_VALUE, false);
        returnError = "Yes".equalsIgnoreCase(preLoadParameter(parameters, _ATTR_RETURN_ERROR, false));
        returnKit = "Yes".equalsIgnoreCase(preLoadParameter(parameters, _ATTR_RETURN_KIT, false));
    }

    public String[] getReturns() {
        return new String[]{_TRANSITION_DONE};
    }

    public String getName() {
        return "Update ESB (v0.2)";
    }

    public String getDescription() {
        return "Update ESB";
    }

}
