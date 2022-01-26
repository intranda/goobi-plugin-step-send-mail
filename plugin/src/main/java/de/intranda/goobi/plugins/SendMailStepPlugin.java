package de.intranda.goobi.plugins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

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
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.goobi.beans.Step;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IStepPluginVersion2;

import de.sub.goobi.config.ConfigPlugins;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class SendMailStepPlugin implements IStepPluginVersion2 {

    @Getter
    private String title = "intranda_step_sendMail";
    @Getter
    private Step step;
    @Getter
    private String value;
    @Getter
    private boolean allowTaskFinishButtons;
    private String returnPath;

    @Override
    public void initialize(Step step, String returnPath) {
        this.returnPath = returnPath;
        this.step = step;

        // read parameters from correct block in configuration file
        SubnodeConfiguration config = ConfigPlugins.getProjectAndStepConfig(title, step);

        String smtpServer = config.getString("smtpServer", null);
        String smtpUser = config.getString("smtpUser", null);
        String smtpPassword = config.getString("smtpPassword", null);
        boolean smtpUseStartTls = config.getBoolean("smtpUseStartTls", false);
        boolean smtpUseSsl = config.getBoolean("smtpUseSsl", false);
        String smtpSenderAddress = config.getString("smtpSenderAddress", null);

        List<String> recipients = Arrays.asList(config.getStringArray("receiver"));

        String messageSubject =  config.getString("messageSubject", null);
        String messageBody =  config.getString("messageBody", null);;

        Properties props = new Properties();
        if (smtpUseStartTls) {
            props.setProperty("mail.transport.protocol", "smtp");
            props.setProperty("mail.smtp.auth", "true");
            props.setProperty("mail.smtp.port", "25");
            props.setProperty("mail.smtp.host", smtpServer);
            props.setProperty("mail.smtp.ssl.trust", "*");
            props.setProperty("mail.smtp.starttls.enable", "true");
            props.setProperty("mail.smtp.starttls.required", "true");
        } else if (smtpUseSsl) {
            props.setProperty("mail.transport.protocol", "smtp");
            props.setProperty("mail.smtp.host", smtpServer);
            props.setProperty("mail.smtp.auth", "true");
            props.setProperty("mail.smtp.port", "465");
            props.setProperty("mail.smtp.ssl.enable", "true");
            props.setProperty("mail.smtp.ssl.trust", "*");
        } else {
            props.setProperty("mail.transport.protocol", "smtp");
            props.setProperty("mail.smtp.auth", "true");
            props.setProperty("mail.smtp.port", "25");
            props.setProperty("mail.smtp.host", smtpServer);
        }
        List<Address> addresses = new ArrayList<>(recipients.size());
        // create a mail for each user
        try {
            for (String receiver : recipients) {
                Address address = new InternetAddress(receiver);
                addresses.add(address);
            }
            Session session = Session.getDefaultInstance(props, null);
            Message msg = new MimeMessage(session);

            InternetAddress addressFrom = new InternetAddress(smtpSenderAddress);
            msg.setFrom(addressFrom);
            //        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(user.getEmail()));
            msg.setRecipients(Message.RecipientType.TO, addresses.toArray(new Address[addresses.size()]));

            // create mail
            MimeMultipart multipart = new MimeMultipart();

            msg.setSubject(messageSubject);
            MimeBodyPart messageHtmlPart = new MimeBodyPart();
            messageHtmlPart.setText(messageBody, "utf-8");
            messageHtmlPart.setHeader("Content-Type", "text/html; charset=\"utf-8\"");
            multipart.addBodyPart(messageHtmlPart);

            msg.setContent(multipart);
            msg.setSentDate(new Date());

            Transport transport = session.getTransport();
            transport.connect(smtpUser, smtpPassword);
            transport.sendMessage(msg, msg.getRecipients(Message.RecipientType.TO));
            transport.close();
        } catch (MessagingException e) {
            log.error(e);
        }

    }

    @Override
    public PluginGuiType getPluginGuiType() {
        return PluginGuiType.NONE;
        // return PluginGuiType.PART;
        // return PluginGuiType.PART_AND_FULL;
        // return PluginGuiType.NONE;
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
        boolean successful = true;
        // your logic goes here

        log.info("SendMail step plugin executed");
        if (!successful) {
            return PluginReturnValue.ERROR;
        }
        return PluginReturnValue.FINISH;
    }
}
