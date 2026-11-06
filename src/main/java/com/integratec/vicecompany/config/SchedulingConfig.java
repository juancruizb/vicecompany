package com.integratec.vicecompany.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa los métodos @Scheduled (tareas programadas), como un cron o Celery beat en Django.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
