# Arquitectura event-driven usando spring boot y kafka

La arquitectura event-driven (orientada a eventos) es un enfoque en el diseño de sistemas de software en el que los componentes se comunican
entre sí mediante el intercambio de eventos. Estos eventos representan cambios de estado, acciones o notificaciones que ocurren dentro del
sistema. La arquitectura event-driven permite que los componentes del sistema respondan de manera eficiente a eventos en tiempo real, lo que
resulta en una mayor agilidad y flexibilidad en el desarrollo de aplicaciones.

## ¿Para qué sirve?

La arquitectura event-driven es especialmente útil en sistemas que requieren una comunicación fluida y asincrónica entre componentes, como
aplicaciones distribuidas, sistemas de microservicios y entornos en tiempo real. En lugar de depender de llamadas directas entre
componentes, la comunicación se basa en la emisión y recepción de eventos, lo que facilita la integración y la adaptación a cambios en el
sistema.

## Ventajas:

- **Desacoplamiento**: Los componentes no necesitan conocer los detalles internos de otros componentes. Esto permite que los cambios se
  realicen
  en un componente sin afectar a otros.
- **Escalabilidad**: La arquitectura event-driven se presta bien para la escalabilidad, ya que los componentes pueden manejar eventos en
  paralelo
  y distribuir la carga.
- **Flexibilidad**: Los componentes pueden reaccionar a eventos de manera dinámica y adaptarse a situaciones cambiantes sin necesidad de
  modificar
  otros componentes.
- **Integración simplificada**: Diferentes sistemas y tecnologías pueden interactuar a través de eventos, lo que facilita la integración en
  entornos heterogéneos.
- **Resiliencia**: Los eventos pueden ser almacenados y reprocesados en caso de fallos, lo que mejora la resiliencia del sistema.

## Desventajas:

- **Complejidad**: La implementación de una arquitectura event-driven puede ser más compleja que enfoques más tradicionales, especialmente
  en
  sistemas pequeños y simples.
- **Gestión de estado**: A veces, rastrear el estado del sistema puede ser más desafiante debido a la naturaleza asincrónica de la
  comunicación.
- **Depuración**: Identificar y solucionar problemas relacionados con eventos puede ser más complejo que en sistemas de comunicación
  síncrona.

## Pasos previos

Antes de comenzar a programar nada vamos a configurarnos un contenedor docker el cual tendra kafka y zookeeper.

Para ello nos creamos un fichero `docker-compose.yml` con la siguiente configuración.

```yaml
version: '3'

services:
  kafka:
    image: wurstmeister/kafka
    container_name: kafka
    ports:
      - "9092:9092"
    environment:
      - KAFKA_ADVERTISED_HOST_NAME=127.0.0.1
      - KAFKA_ADVERTISED_PORT=9092
      - KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181
    depends_on:
      - zookeeper

  zookeeper:
    image: wurstmeister/zookeeper
    ports:
      - "2181:2181"
    environment:
      - KAFKA_ADVERTISED_HOST_NAME=zookeeper
```

Una vez tengamos el fichero podemos arrancarlo usando el comando

```bash
docker compose up
```

## Estructura del proyecto

Vamos a comenzar a generar las bases de nuestro proyecto. En mi caso voy a encapsular el `consumer` y `producer` en una clase padre para
configurar las dependencias de necesarias en el `pom.xml` padre y que el `consumer/pom.xml` y `producer/pom.xml` hereden dichas
dependencias.
El contenido de los `pom.xml` seria la siguiente.

### pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>org.example</groupId>
  <artifactId>event-driven-architectures</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <packaging>pom</packaging>

  <name>Kafka-tutorial-parent</name>
  <description>Tutorial for using Kafka in Java</description>
  <modules>
    <module>producer</module>
    <module>consumer</module>
  </modules>

  <properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <java.version>17</java.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <jackson.version>2.13.0</jackson.version>
  </properties>

  <!-- Configuración del parent que define la configuración base de Spring Boot -->
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.1.2</version>
    <relativePath />
  </parent>

  <dependencies>
    <!-- Jackson -->
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-core</artifactId>
      <version>${jackson.version}</version>
    </dependency>
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
      <version>2.13.4.1</version>
    </dependency>
    <dependency>
      <groupId>com.fasterxml.jackson.datatype</groupId>
      <artifactId>jackson-datatype-jsr310</artifactId>
      <version>${jackson.version}</version>
    </dependency>

    <!--  Spring  -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.kafka</groupId>
      <artifactId>spring-kafka</artifactId>
    </dependency>

    <!--  Tests spring  -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.kafka</groupId>
      <artifactId>spring-kafka-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <!--   Plugin que nos permite arrancar la aplicación con maven   -->
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>

</project>
```

### Producer/pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <!-- Heredamos la configuración del padre -->
  <parent>
    <groupId>org.example</groupId>
    <artifactId>event-driven-architectures</artifactId>
    <version>0.0.1-SNAPSHOT</version>
  </parent>

  <artifactId>producer</artifactId>

  <name>producer</name>
  <description>Producer for Kafka Tutorial</description>

</project>
```

### Consumer/pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <!-- Heredamos la configuración del padre -->
  <parent>
    <groupId>org.example</groupId>
    <artifactId>event-driven-architectures</artifactId>
    <version>0.0.1-SNAPSHOT</version>
  </parent>

  <artifactId>consumer</artifactId>

  <name>consumer</name>
  <description>Consumer for Kafka Tutorial</description>

</project>
```

En este punto nuestro proyecto debe de tener la siguiente estructura.

- `event-driven-architectures`
    - `producer`
        - `src`
        - pom.xml
    - `consumer`
        - `src`
        - pom.xml
    - docker-composer.yml
    - pom.xml

## Producer

### Configurando el Producer

Vamos a comenzar a configurar el proveedor, para ello crearemos las siguientes clases:

- `KafkaProducer`
    - Será la clase responsable de enviar la mensajería.
- `ProducerApplication`
    - Será la clase encargada de arrancar el proveedor.
- `TimestampEvent`
    - Será el evento que se va a enviar, en este caso solo es una clase con un `timestamp`
- `ProducerConfiguration`
    - Será la clase donde configuremos nuestro proveedor.

#### TimestampEvent

```java
import java.io.Serializable;
import java.time.ZonedDateTime;

public record TimestampEvent(ZonedDateTime timestamp) implements Serializable {

}
```

#### KafkaProducer

```java
import java.time.ZonedDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class KafkaProducer {

  private static final Logger LOG = LoggerFactory.getLogger(KafkaProducer.class);

  /**
   * Inyección de dependencia del KafkaTemplate mediante la anotación @Autowired.
   * Permite enviar mensajes a un topic de Kafka de manera simplificada.
   */
  @Autowired
  private KafkaTemplate<String, TimestampEvent> kafkaTemplate;

  /**
   * Método programado que envía eventos de marca de tiempo al topic "timestamp" de Kafka.
   * Se ejecuta cada 5 segundos (según la configuración en 'fixedRate').
   */
  @Scheduled(fixedRate = 5000)
  public void reportCurrentTime() {
    final TimestampEvent event = new TimestampEvent(ZonedDateTime.now());

    // Enviar el evento al topic "timestamp" utilizando el KafkaTemplate.
    this.kafkaTemplate.send("timestamp", event);

    // Registrar en el registro de logs que se ha enviado un evento y su marca de tiempo.
    LOG.info("Sent: {}", event.timestamp());
  }
}
```

#### ProducerConfiguration

```java
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class ProducerConfiguration {

  /**
   * Configuración de propiedades para el productor de Kafka.
   * Define las propiedades clave-valor para la conexión y serialización.
   *
   * @return Mapa de configuración del productor.
   */
  private Map<String, Object> producerConfigs() {
    return Map.of(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
        ConsumerConfig.GROUP_ID_CONFIG, "tutorialGroup"
    );
  }

  /**
   * Bean para crear una fábrica de productores de Kafka personalizada.
   *
   * @return Fábrica de productores de Kafka.
   */
  @Bean
  public ProducerFactory<String, TimestampEvent> producerFactory() {
    return new DefaultKafkaProducerFactory<>(producerConfigs());
  }

  /**
   * Bean para crear un KafkaTemplate que facilita el envío de mensajes a Kafka.
   *
   * @return KafkaTemplate configurado con la fábrica de productores.
   */
  @Bean
  public KafkaTemplate<String, TimestampEvent> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }

  /**
   * Bean para crear un nuevo topic en Kafka utilizando TopicBuilder.
   * Define el nombre del topic como "timestamp".
   *
   * @return Objeto NewTopic configurado.
   */
  @Bean
  public NewTopic timestampTopic() {
    return TopicBuilder.name("timestamp").build();
  }
}
```

#### ProducerApplication

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling// Habilita la ejecución programada de métodos anotados con @Scheduled.
public class ProducerApplication {

  public static void main(String[] args) {
    // Inicia la aplicación Spring Boot con la clase ProducerApplication como argumento.
    SpringApplication.run(ProducerApplication.class, args);
  }
}
```

### Testeo del Producer.

Una vez tenemos todo el código generado vamos a generar una clase de test para comprobar que todo funciona bien, para ello usamos la
utilidad que nos proporciona `spring-boot`.

#### ProducerApplicationTest

```java
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProducerApplicationTest {

  @Test
  void contextLoads() {
  }
}
```

## Consumer

### Configurando el Consumer.

Vamos a comenzar a configurar el consumidor, para ello crearemos las siguientes clases:

- `KafkaConsumer`
    - Será la clase responsable de recibir la mensajería.
- `ConsumerApplication`
    - Será la clase encargada de arrancar el consumidor.
- `TimestampEvent`
    - Será el evento que se va a recibir, en este caso solo es una clase con un `timestamp`
- `ConsumerConfiguration`
    - Será la clase donde configuremos nuestro consumidor.

#### TimestampEvent

```java
import java.io.Serializable;
import java.time.ZonedDateTime;

public record TimestampEvent(ZonedDateTime timestamp) implements Serializable {

}
```

#### KafkaConsumer

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumer.class);

  // Anotación que marca esta función como un listener de Kafka.
  // Cuando un mensaje es recibido en el topic "timestamp", esta función será invocada.
  @KafkaListener(topics = "timestamp", containerFactory = "kafkaListenerContainerFactory")
  void listener(TimestampEvent event) {
    LOGGER.info("Received: {}", event.timestamp());
  }
}

```

#### ConsumerConfiguration

```java
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
public class ConsumerConfiguration {

  // Configuración para el consumidor de Kafka
  private Map<String, Object> consumerConfigs() {
    return Map.of(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",  // Dirección del servidor de Kafka
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,  // Deserializador para claves
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,  // Deserializador para valores en formato JSON
        ConsumerConfig.GROUP_ID_CONFIG, "tutorialGroup"  // ID del grupo de consumidores
    );
  }

  // Creación de la fábrica de consumidores de Kafka
  @Bean
  public ConsumerFactory<String, TimestampEvent> consumerFactory() {
    final JsonDeserializer<TimestampEvent> timestampEventDeserializer = new JsonDeserializer<>(TimestampEvent.class);
    timestampEventDeserializer.setRemoveTypeHeaders(false);
    timestampEventDeserializer.addTrustedPackages("*");
    timestampEventDeserializer.setUseTypeMapperForKey(true);
    return new DefaultKafkaConsumerFactory<>(consumerConfigs(), new StringDeserializer(), timestampEventDeserializer);
  }

  // Creación de la fábrica de contenedores de escucha Kafka
  @Bean
  public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, TimestampEvent>> kafkaListenerContainerFactory() {
    final ConcurrentKafkaListenerContainerFactory<String, TimestampEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    return factory;
  }

  // Creación de un nuevo topic en Kafka llamado "timestamp"
  @Bean
  public NewTopic timestampTopic() {
    return TopicBuilder.name("timestamp")
        .build();
  }
}

```

#### ConsumerApplication

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ConsumerApplication {

  public static void main(String[] args) {
    // Inicia la aplicación Spring Boot para el consumidor.
    // Se especifica la clase principal (ConsumerApplication) y los argumentos (args) que se pasan al inicio.
    SpringApplication.run(ConsumerApplication.class, args);
  }
}
```

### Testeo del Consumer.

```java
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ConsumerApplicationTest {

  @Test
  void contextTest() {
  }
}
```