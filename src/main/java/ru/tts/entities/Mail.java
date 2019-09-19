package ru.tts.entities;

import com.sun.javafx.css.CssError;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Mail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Type(type="text")
    String subject;

    @Type(type="text")
    String receiver;

    @Type(type="text")
    String sender;

    @Temporal(TemporalType.DATE)
    Date send_date;

    @Type(type="text")
    String messageContent;

    @Type(type = "text")
    String contentType;
}
