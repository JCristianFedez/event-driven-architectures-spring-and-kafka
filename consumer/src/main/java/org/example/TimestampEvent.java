package org.example;

import java.io.Serializable;
import java.time.ZonedDateTime;

public record TimestampEvent(ZonedDateTime timestamp) implements Serializable {

}