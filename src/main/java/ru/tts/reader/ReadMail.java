package ru.tts.reader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ru.tts.entities.Mail;
import ru.tts.repositories.MailRepository;

import javax.mail.*;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.io.*;
import java.util.*;


@Service
public class ReadMail {


    private static final String IMAP_AUTH_EMAIL2 = "";
    private static final String IMAP_AUTH_EMAIL1 = "";
    private static final String IMAP_AUTH_EMAIL = "";
    private static final String IMAP_AUTH_EMAIL3 = "";
    private static final String IMAP_AUTH_PWD = "";
    private static final String IMAP_Server = "";
    private static final String IMAP_Port = "993";

    @Value("${save.directory}")
    private String saveDirectory;
    List<String> emails = new ArrayList<String>(Arrays.asList(
            IMAP_AUTH_EMAIL, IMAP_AUTH_EMAIL1, IMAP_AUTH_EMAIL2, IMAP_AUTH_EMAIL3));

    @Autowired
    MailRepository repository;

    @Scheduled(cron = "0 0/1 * * * ?")
    public void readMailFromEmails() {
        for (String email :
                emails) {
            downloadEmails(email);
        }
    }

    public void downloadEmails(String email) {
        Properties properties = new Properties();
        properties.put("mail.debug", "false");
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imap.ssl.enable", "true");
        properties.put("mail.imap.port", IMAP_Port);
        Session session = Session.getDefaultInstance(properties);

        try {
            // connects to the message store
            Store store = session.getStore();
            store.connect(IMAP_Server, email, IMAP_AUTH_PWD);

            // opens the inbox folder
            Folder folderInbox = store.getFolder("INBOX");
            folderInbox.open(Folder.READ_ONLY);

            // fetches new messages from server
            Message[] arrayMessages = folderInbox.getMessages();
            for (Message message :
                    arrayMessages) {
                String from = ((message.getFrom())[0]).toString();
                String subject = message.getSubject();
                String toList = parseAddresses(message.getRecipients(Message.RecipientType.TO));
                Date sentDate = message.getSentDate();
                String contentType = message.getContentType();
                String messageContent = "";
                if (message.isMimeType("text/html")) {
                    messageContent = org.jsoup.Jsoup.parse(message.getContent().toString()).text();
                } else {
                    messageContent = getTextFromMessage(message);
                }

                String contentType1 = message.getContentType();

                if (contentType1.contains("multipart")) {
                    Multipart multiPart = (Multipart) message.getContent();

                    for (int i = 0; i < multiPart.getCount(); i++) {
                        MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(i);
                        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                            //TODO save files
                            System.out.println("Decoding " + decodeString(part.getFileName()));
                        }
                    }
                }

                repository.save(Mail.builder()
                        .sender(from)
                        .subject(subject)
                        .send_date(sentDate)
                        .receiver(toList)
                        .messageContent(messageContent)
                        .contentType(contentType)
                        .build());
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
        String messageText = "";
        if (message.isMimeType("text/plain")) {
            messageText = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            messageText = getTextFromMimeMultipart(mimeMultipart);
        }
        return messageText;
    }

    public static String decodeString(String encodedString) throws UnsupportedEncodingException {
        String result = "";
        String[] strings = encodedString.split("\\?");
        String type = strings[1];
        for (int i = 0; i < strings.length; i++) {
            if ((strings[i]).contains(type)) {
                String prom = new String(Base64.getMimeDecoder().decode(strings[i + 2]), type.toUpperCase());
                result += prom;
            }
        }
        return result;
    }


    private String getTextFromMimeMultipart(
            MimeMultipart mimeMultipart) throws IOException, MessagingException {
        int count = mimeMultipart.getCount();
        if (count == 0)
            throw new MessagingException("Multipart with no body parts not supported.");
        boolean multipartAlt = new ContentType(mimeMultipart.getContentType()).match("multipart");
        if (multipartAlt)
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
