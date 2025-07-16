package de.intranda.goobi.plugins;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.prefs.Preferences;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.exceptions.SwapException;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.easymock.EasyMock;
import org.goobi.api.mail.SendMail;
import org.goobi.beans.*;
import org.goobi.beans.Process;
import org.goobi.production.flow.helper.JobCreation;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import ugh.dl.DigitalDocument;
import ugh.dl.Fileformat;
import ugh.dl.Prefs;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ConfigurationHelper.class, ConfigPlugins.class, SendMail.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*" ,"jdk.internal.reflect.*"})
public class SendMailPluginTest {
    private static String resourcesFolder;

    private SendMail sendMail;
    private Step step;
    private SendMailStepPlugin plugin;
    private SubnodeConfiguration config;

    @BeforeClass
    public static void setUpClass() throws Exception {
        resourcesFolder = "src/test/resources/"; // for junit tests in eclipse

        if (!Files.exists(Paths.get(resourcesFolder))) {
            resourcesFolder = "target/test-classes/"; // to run mvn test from cli or in jenkins
        }

        String log4jFile = resourcesFolder +"log4j2.xml"; // for junit tests in eclipse

        System.setProperty("log4j.configurationFile", log4jFile);
    }

    @Before
    public void setUp() throws IOException, ReadException, SwapException, PreferencesException {
        plugin = new SendMailStepPlugin();
        assertNotNull(plugin);

        DigitalDocument digitalDocument = EasyMock.createMock(DigitalDocument.class);
        EasyMock.replay(digitalDocument);
        Fileformat fileformat = EasyMock.createMock(Fileformat.class);
        EasyMock.expect(fileformat.getDigitalDocument()).andReturn(digitalDocument).anyTimes();
        EasyMock.replay(fileformat);
        Prefs preferences = EasyMock.createMock(Prefs.class);
        EasyMock.replay(preferences);
        Ruleset ruleset = EasyMock.createMock(Ruleset.class);
        EasyMock.expect(ruleset.getPreferences()).andReturn(preferences).anyTimes();
        EasyMock.expect(ruleset.getDatei()).andReturn("ruleset.xml").anyTimes();
        EasyMock.replay(ruleset);

        GoobiProperty mailProperty = EasyMock.createMock(GoobiProperty.class);
        EasyMock.expect(mailProperty.getPropertyName()).andReturn("E-MAIL").anyTimes();
        EasyMock.expect(mailProperty.getPropertyValue()).andReturn("admin@intranda.com; support@intranda.com").anyTimes();
        EasyMock.expect(mailProperty.getContainer()).andReturn("0").anyTimes();
        EasyMock.replay(mailProperty);

        Project project = EasyMock.createMock(Project.class);
        EasyMock.expect(project.getId()).andReturn(1).anyTimes();
        EasyMock.expect(project.getTitel()).andReturn("Project Title").anyTimes();
        EasyMock.expect(project.getProjectIdentifier()).andReturn("proj-1").anyTimes();
        EasyMock.replay(project);
        Process process = EasyMock.createMock(Process.class);
        EasyMock.expect(process.getId()).andReturn(1).anyTimes();
        EasyMock.expect(process.getTitel()).andReturn("Process Title").anyTimes();
        EasyMock.expect(process.isIstTemplate()).andReturn(false).anyTimes();
        EasyMock.expect(process.getMetadataFilePath()).andReturn("/test").anyTimes();
        EasyMock.expect(process.getProjekt()).andReturn(project).anyTimes();
        process.setEigenschaften(null);
        EasyMock.expectLastCall().anyTimes();
        EasyMock.expect(process.getEigenschaften()).andReturn(List.of(mailProperty)).anyTimes();
        EasyMock.expect(process.getEigenschaftenList()).andReturn(List.of(mailProperty)).anyTimes();
        EasyMock.expect(process.readMetadataFile()).andReturn(fileformat).anyTimes();
        EasyMock.expect(process.getRegelsatz()).andReturn(ruleset).anyTimes();
        EasyMock.replay(process);
        step = EasyMock.createMock(Step.class);
        EasyMock.expect(step.getId()).andReturn(1).anyTimes();
        EasyMock.expect(step.getProzess()).andReturn(process).anyTimes();
        EasyMock.expect(step.getTitel()).andReturn("Send Mail").anyTimes();
        EasyMock.replay(step);

        config = EasyMock.createMock(SubnodeConfiguration.class);
        EasyMock.expect(config.getString("messageSubject", any())).andReturn("URGENT - Please read this").anyTimes();
        EasyMock.expect(config.getString("messageBody", any())).andReturn("Haha, I fooled you").anyTimes();
        EasyMock.expect(config.getString("attachment", any())).andReturn("").anyTimes();

        sendMail = EasyMock.createMock(SendMail.class);
        PowerMock.mockStatic(SendMail.class);
        EasyMock.expect(SendMail.getInstance()).andReturn(sendMail).anyTimes();
        PowerMock.replay(SendMail.class);

        PowerMock.mockStatic(ConfigPlugins.class);
        EasyMock.expect(ConfigPlugins.getProjectAndStepConfig("intranda_step_sendMail", step)).andReturn(config).anyTimes();
        PowerMock.replay(ConfigPlugins.class);

        ConfigurationHelper configurationHelper = EasyMock.createMock(ConfigurationHelper.class);
        EasyMock.expect(configurationHelper.getGoobiFolder()).andReturn("/opt/digiverso/goobi").anyTimes();
        EasyMock.expect(configurationHelper.getRulesetFolder()).andReturn("rulesets").anyTimes();
        EasyMock.expect(configurationHelper.getScriptsFolder()).andReturn("scripts").anyTimes();
        EasyMock.expect(configurationHelper.getConfigurationFolder()).andReturn("config").anyTimes();
        EasyMock.replay(configurationHelper);

        PowerMock.mockStatic(ConfigurationHelper.class);
        EasyMock.expect(ConfigurationHelper.getInstance()).andReturn(configurationHelper).anyTimes();
        PowerMock.replay(ConfigurationHelper.class);
    }

    @Test
    public void testMultipleRecipientsFromProperty() {
        String[] receivers = new String[] { "test@intranda.com", "{processes.E-MAIL}" };
        EasyMock.expect(config.getStringArray("receiver")).andReturn(receivers).anyTimes();
        EasyMock.replay(config);
        plugin.initialize(step, "");
        sendMail.sendMailToUser(
                "URGENT - Please read this",
                "Haha, I fooled you",
                List.of("test@intranda.com", "admin@intranda.com", "support@intranda.com"),
                false,
                null
        );
        EasyMock.expectLastCall();
        EasyMock.replay(sendMail);
        plugin.run();
    }

    @Test
    public void testOnlyTheFristRecipientFromProperty() {
        String[] receivers = new String[] { "test@intranda.com", "{process.E-MAIL}" };
        EasyMock.expect(config.getStringArray("receiver")).andReturn(receivers).anyTimes();
        EasyMock.replay(config);
        plugin.initialize(step, "");
        sendMail.sendMailToUser(
                "URGENT - Please read this",
                "Haha, I fooled you",
                List.of("test@intranda.com", "admin@intranda.com"),
                false,
                null
        );
        EasyMock.expectLastCall();
        EasyMock.replay(sendMail);
        plugin.run();
    }
}
