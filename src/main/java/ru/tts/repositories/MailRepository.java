package ru.tts.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.tts.entities.Mail;

import javax.mail.Message;

@Repository
public interface MailRepository extends JpaRepository<Mail, Long> {
}
