package ru.tts.reader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.tts.authenticator.EmailAuthenticator;
import ru.tts.entities.Mail;
import ru.tts.repositories.MailRepository;


import javax.mail.*;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

@Service
public class ReadMail {

    private static final String IMAP_AUTH_EMAIL2 = "";
    private static final String IMAP_AUTH_EMAIL1 = "";
    private static final String IMAP_AUTH_EMAIL = "";
    private static final String IMAP_AUTH_PWD = "";
    private static final String IMAP_Server = "";
    private static final String IMAP_Port = "993";
    private static final String saveDirectory = "";

    List<String> emails = new ArrayList<String>(Arrays.asList(IMAP_AUTH_EMAIL, IMAP_AUTH_EMAIL1, IMAP_AUTH_EMAIL2));

    @Autowired
    MailRepository repository;

    @Scheduled(cron = "0 0/1 * * * ?")
    public void readMailFromEmails() {

        for (String email :
                emails) {
            downloadEmails();
        }

    }

    public void downloadEmails() {
        Properties properties = new Properties();
        properties.put("mail.debug", "false");
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imap.ssl.enable", "true");
        properties.put("mail.imap.port", IMAP_Port);
        Session session = Session.getDefaultInstance(properties);

        try {
            // connects to the message store
            Store store = session.getStore();
            store.connect(IMAP_Server, IMAP_AUTH_EMAIL, IMAP_AUTH_PWD);

            // opens the inbox folder
            Folder folderInbox = store.getFolder("INBOX");
            folderInbox.open(Folder.READ_ONLY);

            // fetches new messages from server
            Message[] arrayMessages = folderInbox.getMessages();
            for (Message message :
                    arrayMessages) {
                String from = ((message.getFrom())[0]).toString();;
                String subject = message.getSubject();
                String toList = parseAddresses(message.getRecipients(Message.RecipientType.TO));
                Date sentDate = message.getSentDate();
                String contentType = message.getContentType();
                String messageContent = "";
                if (message.isMimeType("text/html")){
                    messageContent = org.jsoup.Jsoup.parse(message.getContent().toString()).text();
                } else {
                    messageContent = getTextFromMessage(message);
                }

                repository.save(Mail.builder()
                        .sender(from)
                        .subject(subject)
                        .send_date(sentDate)
                        .receiver(toList)
                        .messageContent(messageContent)
                        .contentType(contentType)
                        .build());
                System.out.println("\t From: " + from);
                System.out.println("\t Content-Type: " + contentType);
                System.out.println("\t To: " + toList);
                System.out.println("\t Subject: " + subject);
                System.out.println("\t Sent Date: " + sentDate);
                System.out.println("\t Message: " + messageContent);

            }
            // disconnect
            folderInbox.close(false);
            store.close();
        } catch (NoSuchProviderException ex) {
            System.out.println("No provider for imap");
            ex.printStackTrace();
        } catch (MessagingException ex) {
            System.out.println("Could not connect to the message store");
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getTextFromMessage(Message message) throws IOException, MessagingException {
        String result = "";
        if (message.isMimeType("text/plain")) {
            result = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            result = getTextFromMimeMultipart(mimeMultipart);
        }
        return result;
    }

    private String getTextFromMimeMultipart(
            MimeMultipart mimeMultipart) throws IOException, MessagingException {

        int count = mimeMultipart.getCount();
        if (count == 0)
            throw new MessagingException("Multipart with no body parts not supported.");
        boolean multipartAlt = new ContentType(mimeMultipart.getContentType()).match("multipart/alternative");
        if (multipartAlt)
            // alternatives appear in an order of increasing
            // faithfulness to the original content. Customize as req'd.
            return getTextFromBodyPart(mimeMultipart.getBodyPart(count - 1));
        String result = "";
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            result += getTextFromBodyPart(bodyPart);
        }
        return result;
    }

    private String getTextFromBodyPart(
            BodyPart bodyPart) throws IOException, MessagingException {

        String result = "";
        if (bodyPart.isMimeType("text/plain")) {
            result = (String) bodyPart.getContent();
        } else if (bodyPart.isMimeType("text/html")) {
            String html = (String) bodyPart.getContent();
            result = org.jsoup.Jsoup.parse(html).text();
        } else if (bodyPart.getContent() instanceof MimeMultipart) {
            result = getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent());
        }
        return result;
    }

    private String parseAddresses(Address[] address) {
        String listAddress = "";

        if (address != null) {
            for (int i = 0; i < address.length; i++) {
                listAddress += address[i].toString() + ", ";
            }
        }
        if (listAddress.length() > 1) {
            listAddress = listAddress.substring(0, listAddress.length() - 2);
        }

        return listAddress;
    }
}
