package de.intranda.goobi.plugins;

import java.io.IOException;
import java.util.Arrays;

/**
 * This file is part of a plugin for Goobi - a Workflow tool for the support of mass digitization.
 *
 * Visit the websites for more information.
 *          - https://goobi.io
 *          - https://www.intranda.com
 *          - https://github.com/intranda/goobi
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

import java.util.HashMap;
import java.util.List;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.goobi.api.mail.SendMail;
import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IStepPluginVersion2;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.VariableReplacer;
import de.sub.goobi.helper.exceptions.SwapException;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DigitalDocument;
import ugh.dl.Fileformat;
import ugh.exceptions.UGHException;

@PluginImplementation
@Log4j2
public class SendMailStepPlugin implements IStepPluginVersion2 {

    private static final long serialVersionUID = -2248755432954006453L;

    @Getter
    private String title = "intranda_step_sendMail";
    @Getter
    private Step step;
    private Process process;

    private String returnPath;

    //    private String smtpServer;
    //    private String smtpUser;
    //    private String smtpPassword;
    //    private boolean smtpUseStartTls;
    //    private boolean smtpUseSsl;
    //    private String smtpSenderAddress;
    private List<String> recipients;
    private String messageSubject;
    private String messageBody;

    @Override
    public void initialize(Step step, String returnPath) {
        this.returnPath = returnPath;
        this.step = step;
        process = step.getProzess();

        // read parameters from correct block in configuration file
        SubnodeConfiguration config = ConfigPlugins.getProjectAndStepConfig(title, step);

        //        smtpServer = config.getString("smtpServer", null);
        //        smtpUser = config.getString("smtpUser", null);
        //        smtpPassword = config.getString("smtpPassword", null);
        //        smtpUseStartTls = config.getBoolean("smtpUseStartTls", false);
        //        smtpUseSsl = config.getBoolean("smtpUseSsl", false);
        //        smtpSenderAddress = config.getString("smtpSenderAddress", null);
        //
        recipients = Arrays.asList(config.getStringArray("receiver"));

        messageSubject = config.getString("messageSubject", null);
        messageBody = config.getString("messageBody", null);

    }

    @Override
    public PluginGuiType getPluginGuiType() {
        return PluginGuiType.NONE;
    }

    @Override
    public String getPagePath() {
        return null;
    }

    @Override
    public PluginType getType() {
        return PluginType.Step;
    }

    @Override
    public String cancel() {
        return "/uii" + returnPath;
    }

    @Override
    public String finish() {
        return "/uii" + returnPath;
    }

    @Override
    public int getInterfaceVersion() {
        return 0;
    }

    @Override
    public HashMap<String, StepReturnValue> validate() {
        return null;
    }

    @Override
    public boolean execute() {
        PluginReturnValue ret = run();
        return ret != PluginReturnValue.ERROR;
    }

    @Override
    public PluginReturnValue run() {

        String subject = messageSubject;
        String body = messageBody;
        try {
            Fileformat ff = process.readMetadataFile();
            DigitalDocument dd = ff.getDigitalDocument();
            VariableReplacer vr = new VariableReplacer(dd, process.getRegelsatz().getPreferences(), process, step);

            subject = vr.replace(subject);
            body = vr.replace(body);
        } catch (UGHException | IOException | SwapException e1) {
            log.error(e1);
        }

        SendMail.getInstance().sendMailToUser(subject, body, recipients, false);
        log.debug("SendMail step plugin executed");
        return PluginReturnValue.FINISH;
    }
}
