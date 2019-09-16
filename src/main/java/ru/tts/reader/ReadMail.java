package ru.tts.reader;

import ru.tts.authenticator.EmailAuthenticator;
import ru.tts.entities.Mail;


import javax.mail.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;


public class ReadMail {
    String IMAP_AUTH_EMAIL = "";
    String IMAP_AUTH_PWD = "";
    String IMAP_Server = "";
    String IMAP_Port = "993";

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public ReadMail() {
        Properties properties = new Properties();
        properties.put("mail.debug", "false");
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imap.ssl.enable", "true");
        properties.put("mail.imap.port", IMAP_Port);

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

            // Открываем папку в режиме только для чтения
            inbox.open(Folder.READ_ONLY);
            BufferedWriter writer = new BufferedWriter(new FileWriter(""));
            writer.write("Количество сообщений : " +
                    String.valueOf(inbox.getMessageCount()));
            if (inbox.getMessageCount() == 0){
                return;
            }
            for (int i = 1; i < inbox.getMessageCount(); i++){
//                writer.write(inbox.getMessage(i).getSubject());
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
//            for (int i = 0; i < mp.getCount(); i++) {
//                BodyPart bp = mp.getBodyPart(i);
//                if (bp.getFileName() == null)
//                    writer.write("    " + i + ". сообщение : '" +
//                            bp.getContent() + "'");
//                else
//                    writer.write("    " + i + ". файл : '" +
//                            bp.getFileName() + "'");
//            }
            writer.flush();
            writer.close();
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
