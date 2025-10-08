/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.theplutushome.veristore.email;

/**
 *
 * @author MalickMoro-Samah
 */
import freemarker.template.Configuration;
import freemarker.template.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import java.io.StringWriter;
import java.util.Map;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.Locale;
import java.text.MessageFormat;

@ApplicationScoped
public class EmailTemplateService {
    
    private Configuration configuration;
    
    @PostConstruct
    public void init() {
        configuration = new Configuration(Configuration.VERSION_2_3_32);
        configuration.setClassForTemplateLoading(this.getClass(), "/email-templates");
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);
        configuration.setWrapUncheckedExceptions(true);
        configuration.setFallbackOnNullLoopVariable(false);
    }
    
    /**
     * Generate HTML email from template
     * @param templateName
     * @param data
     * @return String
     * @throws java.lang.Exception
     */
    public String generateHtmlEmail(String templateName, EmailData data) throws Exception {
        Template template = configuration.getTemplate(templateName + ".html");
        return processTemplate(template, data);
    }
    
    /**
     * Generate TEXT email from template
     * @param templateName
     * @param data
     * @return String
     * @throws java.lang.Exception
     */
    public String generateTextEmail(String templateName, EmailData data) throws Exception {
        Template template = configuration.getTemplate(templateName + ".txt");
        return processTemplate(template, data);
    }
    
    /**
     * Generate email from any template file
     * @param templateFileName
     * @param data
     * @return String
     * @throws java.lang.Exception
     */
    public String generateEmail(String templateFileName, EmailData data) throws Exception {
        Template template = configuration.getTemplate(templateFileName);
        return processTemplate(template, data);
    }
    
    private String processTemplate(Template template, EmailData data) throws Exception {
        Map<String, Object> model = createTemplateModel(data);
        
        StringWriter writer = new StringWriter();
        template.process(model, writer);
        return writer.toString();
    }
    
    private ResourceBundle getBundle(Locale locale) {
        return ResourceBundle.getBundle("messages", locale);
    }

    private String getLocalizedString(String key, Locale locale, Object... params) {
        ResourceBundle bundle = getBundle(locale);
        String pattern = bundle.getString(key);
        return MessageFormat.format(pattern, params);
    }
    
    private Map<String, Object> createTemplateModel(EmailData data) {
        Map<String, Object> model = new HashMap<>();
        
        // Email data
        model.put("emailData", data);
        model.put("name", data.getName());
        model.put("recipient", data.getRecipient());
        model.put("otpCode", data.getOtpCode());
        model.put("temporaryPassword", data.getTemporaryPassword());
        model.put("applicationId", data.getApplicationId());
        model.put("date", data.getDate());
        model.put("location", data.getLocation());
        model.put("userAgent", data.getUserAgent());
        model.put("timeSlot", data.getTimeSlot());
        
        // Company info
        model.put("companyName", "Hyperion");
        model.put("tagline", "NIA Enrollment Platform");
        model.put("teamName", "Hyperion Team");
        model.put("supportEmail", "support@hyperion.com");
        model.put("website", "https://www.nia.gov.gh");
        
        // Computed values
        String currentYear = String.valueOf(java.time.Year.now().getValue());
        model.put("currentYear", currentYear.replaceAll(",", ""));
        model.put("hasOtp", data.getOtpCode() != null && !data.getOtpCode().isEmpty());
        model.put("hasApplication", data.getApplicationId() != null && !data.getApplicationId().isEmpty());
        
        return model;
    }
    
    private Map<String, Object> createTemplateModel(EmailData data, Locale locale) {
        Map<String, Object> model = createTemplateModel(data);
        // Add localized strings
        model.put("greeting", getLocalizedString("email.greeting", locale, data.getName()));
        // Add more localized fields as needed
        return model;
    }
    
    // Specific email generation methods
    public String generateSignUpOtpHtml(EmailData data) throws Exception {
        return generateHtmlEmail("signup-otp", data);
    }
    
    public String generateSignUpOtpText(EmailData data) throws Exception {
        return generateTextEmail("signup-otp", data);
    }
    
    public String generateSignUpSuccessHtml(EmailData data) throws Exception {
        return generateHtmlEmail("signup-success", data);
    }
    
    public String generateLoginAlertHtml(EmailData data) throws Exception {
        return generateHtmlEmail("login-alert", data);
    }
    
    public String generateLoginAlertText(EmailData data) throws Exception {
        return generateTextEmail("login-alert", data);
    }
    
    public String generatePasswordChangedText(EmailData data) throws Exception {
        return generateTextEmail("password-changed", data);
    }
    
    public String generatePasswordResetOtptHtml(EmailData data) throws Exception {
        return generateHtmlEmail("password-reset-otp", data);
    }
    
    public String generatePasswordResetOtpText(EmailData data) throws Exception {
        return generateTextEmail("password-reset-otp", data);
    }

    // Overloaded methods to support locale
    public String generateHtmlEmail(String templateName, EmailData data, Locale locale) throws Exception {
        Template template = configuration.getTemplate(templateName + ".html");
        Map<String, Object> model = createTemplateModel(data, locale);
        StringWriter writer = new StringWriter();
        template.process(model, writer);
        return writer.toString();
    }

    public String generateTextEmail(String templateName, EmailData data, Locale locale) throws Exception {
        Template template = configuration.getTemplate(templateName + ".txt");
        Map<String, Object> model = createTemplateModel(data, locale);
        StringWriter writer = new StringWriter();
        template.process(model, writer);
        return writer.toString();
    }
}