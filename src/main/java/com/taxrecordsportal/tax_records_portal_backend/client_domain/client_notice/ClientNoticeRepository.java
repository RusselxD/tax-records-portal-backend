package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_notice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClientNoticeRepository extends JpaRepository<ClientNotice, Integer> {

    List<ClientNotice> findByClientIdOrderByIdDesc(UUID clientId);
}
