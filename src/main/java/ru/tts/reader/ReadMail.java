package ru.tts.reader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.tts.authenticator.EmailAuthenticator;
import ru.tts.entities.Mail;
import ru.tts.repositories.MailRepository;


import javax.mail.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

@Service
public class ReadMail {

    private static final String IMAP_AUTH_EMAIL2 = "";
    private static final String IMAP_AUTH_EMAIL1 = "";
    private static final String IMAP_AUTH_EMAIL = "";
    private static final String IMAP_AUTH_PWD = "";
    private static final String IMAP_Server = "";
    private static final String IMAP_Port = "993";

    List<String> emails = new ArrayList<String>(Arrays.asList(IMAP_AUTH_EMAIL, IMAP_AUTH_EMAIL1, IMAP_AUTH_EMAIL2));

    @Autowired
    MailRepository repository;

    @Scheduled(cron = "0 0/5 * * * ?")
    public void readMailFromEmails() {
        Properties properties = new Properties();
        properties.put("mail.debug", "false");
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imap.ssl.enable", "true");
        properties.put("mail.imap.port", IMAP_Port);

        for (String email :
                emails) {
            readingAndSave(properties);
        }

    }

    public void readingAndSave(Properties properties) {
        Authenticator auth = new EmailAuthenticator(IMAP_AUTH_EMAIL,
                IMAP_AUTH_PWD);
        Session session = Session.getDefaultInstance(properties, auth);
        session.setDebug(false);
        try {
            Store store = session.getStore();

            // Подключение к почтовому серверу
            store.connect(IMAP_Server, IMAP_AUTH_EMAIL, IMAP_AUTH_PWD);

            // Папка входящих сообщений
            Folder inbox = store.getFolder("INBOX");

            // Открываем папку в режиме чтения и изменения
            inbox.open(Folder.READ_WRITE);
            if (inbox.getMessageCount() == 0) {
                return;
            }
            for (int i = 1; i < inbox.getMessageCount(); i++) {
                System.out.println(inbox.getMessage(i).getSubject());
                Mail mail = Mail.builder()
                        .receiver(inbox.getMessage(i).getSubject())
                        .sender(Arrays.toString(inbox.getMessage(i).getFrom()))
                        .subject(inbox.getMessage(i).getSubject())
                        .send_date(inbox.getMessage(i).getSentDate())
                        .build();
                repository.save(mail);
            }
            // Последнее сообщение; первое сообщение под номером 1
            Message message = inbox.getMessage(inbox.getMessageCount());
            Multipart mp = (Multipart) message.getContent();

            // Вывод содержимого в консоль
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i);
                if (bp.getFileName() == null)
                    System.out.println("    " + i + ". сообщение : '" +
                            bp.getContent() + "'");
                else
                    System.out.println("    " + i + ". файл : '" +
                            bp.getFileName() + "'");
            }
        } catch (NoSuchProviderException e) {
            System.err.println(e.getMessage());
        } catch (MessagingException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        }

    }
}
