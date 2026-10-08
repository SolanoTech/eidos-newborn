--
-- PostgreSQL database dump
--


-- Dumped from database version 16.14 (Debian 16.14-1.pgdg13+1)
-- Dumped by pg_dump version 16.14 (Debian 16.14-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: pg_trgm; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;


--
-- Name: EXTENSION pg_trgm; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION pg_trgm IS 'text similarity measurement and index searching based on trigrams';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: golden_record; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.golden_record (
    gr_client_id character varying(255) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    gr_addr_perm_reg_address character varying(255),
    gr_addr_perm_reg_cadastre character varying(255),
    gr_addr_perm_reg_country character varying(255),
    gr_addr_perm_reg_country_id character varying(255),
    gr_addr_perm_reg_district character varying(255),
    gr_addr_perm_reg_district_id character varying(255),
    gr_addr_perm_reg_mfy character varying(255),
    gr_addr_perm_reg_mfy_id character varying(255),
    gr_addr_perm_reg_region character varying(255),
    gr_addr_perm_reg_region_id character varying(255),
    gr_addr_perm_reg_registration_date date,
    gr_addr_permanent_address character varying(255),
    gr_addr_temp_reg_address character varying(255),
    gr_addr_temp_reg_date_from date,
    gr_addr_temp_reg_date_till date,
    gr_addr_temp_reg_district character varying(255),
    gr_addr_temp_reg_district_id character varying(255),
    gr_addr_temp_reg_mfy character varying(255),
    gr_addr_temp_reg_mfy_id character varying(255),
    gr_addr_temp_reg_region character varying(255),
    gr_addr_temp_reg_region_id character varying(255),
    gr_addr_temporary_address character varying(255),
    gr_birth_country character varying(255),
    gr_birth_country_id character varying(255),
    gr_birth_date date NOT NULL,
    gr_birth_place character varying(255),
    gr_citizenship character varying(255) NOT NULL,
    gr_citizenship_id character varying(255) NOT NULL,
    gr_contacts_email character varying(255),
    gr_doc_expiry_date date,
    gr_doc_issued_by character varying(255) NOT NULL,
    gr_doc_issued_by_id character varying(255) NOT NULL,
    gr_doc_issued_date date NOT NULL,
    gr_doc_pass_data character varying(9) NOT NULL,
    gr_first_name character varying(255) NOT NULL,
    gr_gender character varying(8) NOT NULL,
    gr_last_name character varying(255) NOT NULL,
    gr_middle_name character varying(255),
    gr_mobile_phone_main character varying(12) NOT NULL,
    gr_nationality character varying(255),
    gr_nationality_id character varying(255),
    gr_pinfl character varying(14) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version integer NOT NULL,
    CONSTRAINT golden_record_gr_gender_check CHECK (((gr_gender)::text = ANY ((ARRAY['MALE'::character varying, 'FEMALE'::character varying])::text[])))
);


--
-- Name: golden_record_archive; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.golden_record_archive (
    id bigint NOT NULL,
    gr_addr_perm_reg_address character varying(255),
    gr_addr_perm_reg_cadastre character varying(255),
    gr_addr_perm_reg_country character varying(255),
    gr_addr_perm_reg_country_id character varying(255),
    gr_addr_perm_reg_district character varying(255),
    gr_addr_perm_reg_district_id character varying(255),
    gr_addr_perm_reg_mfy character varying(255),
    gr_addr_perm_reg_mfy_id character varying(255),
    gr_addr_perm_reg_region character varying(255),
    gr_addr_perm_reg_region_id character varying(255),
    gr_addr_perm_reg_registration_date date,
    gr_addr_permanent_address character varying(255),
    gr_addr_temp_reg_address character varying(255),
    gr_addr_temp_reg_date_from date,
    gr_addr_temp_reg_date_till date,
    gr_addr_temp_reg_district character varying(255),
    gr_addr_temp_reg_district_id character varying(255),
    gr_addr_temp_reg_mfy character varying(255),
    gr_addr_temp_reg_mfy_id character varying(255),
    gr_addr_temp_reg_region character varying(255),
    gr_addr_temp_reg_region_id character varying(255),
    gr_addr_temporary_address character varying(255),
    gr_birth_country character varying(255),
    gr_birth_country_id character varying(255),
    gr_birth_date date,
    gr_birth_place character varying(255),
    gr_citizenship character varying(255),
    gr_citizenship_id character varying(255),
    gr_contacts_email character varying(255),
    gr_doc_expiry_date date,
    gr_doc_issued_by character varying(255),
    gr_doc_issued_by_id character varying(255),
    gr_doc_issued_date date,
    gr_doc_pass_data character varying(9),
    gr_first_name character varying(255),
    gr_gender character varying(8),
    gr_last_name character varying(255),
    gr_middle_name character varying(255),
    gr_mobile_phone_main character varying(12),
    gr_nationality character varying(255),
    gr_nationality_id character varying(255),
    gr_pinfl character varying(14),
    archived_at timestamp(6) without time zone NOT NULL,
    archived_version integer NOT NULL,
    gr_client_id character varying(255) NOT NULL,
    CONSTRAINT golden_record_archive_gr_gender_check CHECK (((gr_gender)::text = ANY ((ARRAY['MALE'::character varying, 'FEMALE'::character varying])::text[])))
);


--
-- Name: golden_record_archive_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.golden_record_archive ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.golden_record_archive_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: golden_record_external_id; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.golden_record_external_id (
    id integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    external_id character varying(255) NOT NULL,
    is_active boolean NOT NULL,
    gr_client_id character varying(255) NOT NULL,
    source_id integer NOT NULL
);


--
-- Name: golden_record_external_id_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.golden_record_external_id ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.golden_record_external_id_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: golden_record_field_meta; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.golden_record_field_meta (
    field_name character varying(100) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    gr_client_id character varying(255) NOT NULL,
    source_id integer NOT NULL
);


--
-- Name: legal_entity_archive; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.legal_entity_archive (
    id bigint NOT NULL,
    gr_activity_state_code smallint,
    gr_activity_state_detail_id smallint,
    gr_activity_state_name character varying(255),
    gr_address_full character varying(500),
    gr_buildings_total integer,
    gr_business_type_id smallint,
    gr_business_type_name character varying(255),
    gr_cadastres_total integer,
    gr_connections_total integer,
    gr_courts_total integer,
    gr_deals_customer_total integer,
    gr_deals_provider_total integer,
    gr_director_name character varying(500),
    gr_director_uuid character varying(64),
    gr_district_code integer,
    gr_district_name character varying(255),
    gr_email character varying(255),
    gr_email_status smallint,
    gr_flat character varying(255),
    gr_founders text,
    gr_founders_count smallint,
    gr_full_name character varying(500),
    gr_house character varying(255),
    gr_inn character varying(9),
    gr_is_active boolean,
    gr_is_bankrupt boolean,
    gr_is_dishonest_executor boolean,
    gr_is_small_business boolean,
    gr_is_supplier boolean,
    gr_is_vat_abuser boolean,
    gr_is_vat_payer boolean,
    gr_kfs_code smallint,
    gr_kfs_name character varying(255),
    gr_licenses_total integer,
    gr_oked_code character varying(15),
    gr_oked_name character varying(500),
    gr_oked_name_uz character varying(500),
    gr_opf_code character varying(15),
    gr_opf_name character varying(500),
    gr_opf_name_uz character varying(500),
    gr_phones text,
    gr_postcode character varying(255),
    gr_region_code integer,
    gr_region_name character varying(255),
    gr_registration_authority character varying(500),
    gr_registration_date date,
    gr_registration_number character varying(15),
    gr_short_name character varying(255),
    gr_soato_code character varying(15),
    gr_soato_name character varying(500),
    gr_soogu_code character varying(5),
    gr_soogu_name character varying(500),
    gr_statutory_fund numeric(17,2),
    gr_street_name character varying(500),
    gr_tax_mode smallint,
    gr_trust_rating character varying(10),
    gr_trust_score smallint,
    gr_vat_number character varying(20),
    gr_village_code integer,
    gr_village_name character varying(255),
    archived_at timestamp(6) without time zone NOT NULL,
    archived_version integer NOT NULL,
    gr_legal_entity_id character varying(255) NOT NULL
);


--
-- Name: legal_entity_archive_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.legal_entity_archive ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.legal_entity_archive_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: legal_entity_external_id; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.legal_entity_external_id (
    id integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    external_id character varying(255) NOT NULL,
    is_active boolean NOT NULL,
    gr_legal_entity_id character varying(255) NOT NULL,
    source_id integer NOT NULL
);


--
-- Name: legal_entity_external_id_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.legal_entity_external_id ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.legal_entity_external_id_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: legal_entity_field_meta; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.legal_entity_field_meta (
    field_name character varying(100) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    gr_legal_entity_id character varying(255) NOT NULL,
    source_id integer NOT NULL
);


--
-- Name: legal_entity_record; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.legal_entity_record (
    gr_legal_entity_id character varying(255) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    gr_activity_state_code smallint,
    gr_activity_state_detail_id smallint,
    gr_activity_state_name character varying(255),
    gr_address_full character varying(500),
    gr_buildings_total integer,
    gr_business_type_id smallint,
    gr_business_type_name character varying(255),
    gr_cadastres_total integer,
    gr_connections_total integer,
    gr_courts_total integer,
    gr_deals_customer_total integer,
    gr_deals_provider_total integer,
    gr_director_name character varying(500),
    gr_director_uuid character varying(64),
    gr_district_code integer,
    gr_district_name character varying(255),
    gr_email character varying(255),
    gr_email_status smallint,
    gr_flat character varying(255),
    gr_founders text,
    gr_founders_count smallint,
    gr_full_name character varying(500) NOT NULL,
    gr_house character varying(255),
    gr_inn character varying(9) NOT NULL,
    gr_is_active boolean NOT NULL,
    gr_is_bankrupt boolean NOT NULL,
    gr_is_dishonest_executor boolean,
    gr_is_small_business boolean,
    gr_is_supplier boolean,
    gr_is_vat_abuser boolean,
    gr_is_vat_payer boolean,
    gr_kfs_code smallint,
    gr_kfs_name character varying(255),
    gr_licenses_total integer,
    gr_oked_code character varying(15) NOT NULL,
    gr_oked_name character varying(500) NOT NULL,
    gr_oked_name_uz character varying(500),
    gr_opf_code character varying(15),
    gr_opf_name character varying(500),
    gr_opf_name_uz character varying(500),
    gr_phones text,
    gr_postcode character varying(255),
    gr_region_code integer,
    gr_region_name character varying(255),
    gr_registration_authority character varying(500),
    gr_registration_date date,
    gr_registration_number character varying(15),
    gr_short_name character varying(255),
    gr_soato_code character varying(15),
    gr_soato_name character varying(500),
    gr_soogu_code character varying(5),
    gr_soogu_name character varying(500),
    gr_statutory_fund numeric(17,2),
    gr_street_name character varying(500),
    gr_tax_mode smallint,
    gr_trust_rating character varying(10),
    gr_trust_score smallint,
    gr_vat_number character varying(20),
    gr_village_code integer,
    gr_village_name character varying(255),
    name_normalized character varying(512),
    updated_at timestamp(6) without time zone NOT NULL,
    version integer NOT NULL
);


--
-- Name: source; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.source (
    id integer NOT NULL,
    source_name character varying(255) NOT NULL,
    trust_level integer NOT NULL
);


--
-- Name: source_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.source ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.source_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tentative_golden_record; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tentative_golden_record (
    id bigint NOT NULL,
    gr_addr_perm_reg_address character varying(255),
    gr_addr_perm_reg_cadastre character varying(255),
    gr_addr_perm_reg_country character varying(255),
    gr_addr_perm_reg_country_id character varying(255),
    gr_addr_perm_reg_district character varying(255),
    gr_addr_perm_reg_district_id character varying(255),
    gr_addr_perm_reg_mfy character varying(255),
    gr_addr_perm_reg_mfy_id character varying(255),
    gr_addr_perm_reg_region character varying(255),
    gr_addr_perm_reg_region_id character varying(255),
    gr_addr_perm_reg_registration_date date,
    gr_addr_permanent_address character varying(255),
    gr_addr_temp_reg_address character varying(255),
    gr_addr_temp_reg_date_from date,
    gr_addr_temp_reg_date_till date,
    gr_addr_temp_reg_district character varying(255),
    gr_addr_temp_reg_district_id character varying(255),
    gr_addr_temp_reg_mfy character varying(255),
    gr_addr_temp_reg_mfy_id character varying(255),
    gr_addr_temp_reg_region character varying(255),
    gr_addr_temp_reg_region_id character varying(255),
    gr_addr_temporary_address character varying(255),
    gr_birth_country character varying(255),
    gr_birth_country_id character varying(255),
    gr_birth_date date,
    gr_birth_place character varying(255),
    gr_citizenship character varying(255),
    gr_citizenship_id character varying(255),
    gr_contacts_email character varying(255),
    gr_doc_expiry_date date,
    gr_doc_issued_by character varying(255),
    gr_doc_issued_by_id character varying(255),
    gr_doc_issued_date date,
    gr_doc_pass_data character varying(9),
    gr_first_name character varying(255),
    gr_gender character varying(8),
    gr_last_name character varying(255),
    gr_middle_name character varying(255),
    gr_mobile_phone_main character varying(12),
    gr_nationality character varying(255),
    gr_nationality_id character varying(255),
    gr_pinfl character varying(14),
    created_at timestamp(6) without time zone NOT NULL,
    gr_client_id character varying(255),
    reason character varying(32) NOT NULL,
    source_name character varying(255),
    CONSTRAINT tentative_golden_record_gr_gender_check CHECK (((gr_gender)::text = ANY ((ARRAY['MALE'::character varying, 'FEMALE'::character varying])::text[]))),
    CONSTRAINT tentative_golden_record_reason_check CHECK (((reason)::text = ANY ((ARRAY['UNKNOWN_SOURCE'::character varying, 'GREY_ZONE_CONFLICT'::character varying])::text[])))
);


--
-- Name: tentative_golden_record_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tentative_golden_record ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tentative_golden_record_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tentative_legal_entity; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tentative_legal_entity (
    id bigint NOT NULL,
    gr_activity_state_code smallint,
    gr_activity_state_detail_id smallint,
    gr_activity_state_name character varying(255),
    gr_address_full character varying(500),
    gr_buildings_total integer,
    gr_business_type_id smallint,
    gr_business_type_name character varying(255),
    gr_cadastres_total integer,
    gr_connections_total integer,
    gr_courts_total integer,
    gr_deals_customer_total integer,
    gr_deals_provider_total integer,
    gr_director_name character varying(500),
    gr_director_uuid character varying(64),
    gr_district_code integer,
    gr_district_name character varying(255),
    gr_email character varying(255),
    gr_email_status smallint,
    gr_flat character varying(255),
    gr_founders text,
    gr_founders_count smallint,
    gr_full_name character varying(500),
    gr_house character varying(255),
    gr_inn character varying(9),
    gr_is_active boolean,
    gr_is_bankrupt boolean,
    gr_is_dishonest_executor boolean,
    gr_is_small_business boolean,
    gr_is_supplier boolean,
    gr_is_vat_abuser boolean,
    gr_is_vat_payer boolean,
    gr_kfs_code smallint,
    gr_kfs_name character varying(255),
    gr_licenses_total integer,
    gr_oked_code character varying(15),
    gr_oked_name character varying(500),
    gr_oked_name_uz character varying(500),
    gr_opf_code character varying(15),
    gr_opf_name character varying(500),
    gr_opf_name_uz character varying(500),
    gr_phones text,
    gr_postcode character varying(255),
    gr_region_code integer,
    gr_region_name character varying(255),
    gr_registration_authority character varying(500),
    gr_registration_date date,
    gr_registration_number character varying(15),
    gr_short_name character varying(255),
    gr_soato_code character varying(15),
    gr_soato_name character varying(500),
    gr_soogu_code character varying(5),
    gr_soogu_name character varying(500),
    gr_statutory_fund numeric(17,2),
    gr_street_name character varying(500),
    gr_tax_mode smallint,
    gr_trust_rating character varying(10),
    gr_trust_score smallint,
    gr_vat_number character varying(20),
    gr_village_code integer,
    gr_village_name character varying(255),
    created_at timestamp(6) without time zone NOT NULL,
    gr_legal_entity_id character varying(255),
    reason character varying(32) NOT NULL,
    source_name character varying(255),
    CONSTRAINT tentative_legal_entity_reason_check CHECK (((reason)::text = ANY ((ARRAY['UNKNOWN_SOURCE'::character varying, 'GREY_ZONE_CONFLICT'::character varying])::text[])))
);


--
-- Name: tentative_legal_entity_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tentative_legal_entity ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tentative_legal_entity_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: golden_record_archive golden_record_archive_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.golden_record_archive
    ADD CONSTRAINT golden_record_archive_pkey PRIMARY KEY (id);


--
-- Name: golden_record_external_id golden_record_external_id_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.golden_record_external_id
    ADD CONSTRAINT golden_record_external_id_pkey PRIMARY KEY (id);


--
-- Name: golden_record_field_meta golden_record_field_meta_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.golden_record_field_meta
    ADD CONSTRAINT golden_record_field_meta_pkey PRIMARY KEY (field_name, gr_client_id);


--
-- Name: golden_record golden_record_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.golden_record
    ADD CONSTRAINT golden_record_pkey PRIMARY KEY (gr_client_id);


--
-- Name: legal_entity_archive legal_entity_archive_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.legal_entity_archive
    ADD CONSTRAINT legal_entity_archive_pkey PRIMARY KEY (id);


--
-- Name: legal_entity_external_id legal_entity_external_id_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.legal_entity_external_id
    ADD CONSTRAINT legal_entity_external_id_pkey PRIMARY KEY (id);


--
-- Name: legal_entity_field_meta legal_entity_field_meta_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.legal_entity_field_meta
    ADD CONSTRAINT legal_entity_field_meta_pkey PRIMARY KEY (field_name, gr_legal_entity_id);


--
-- Name: legal_entity_record legal_entity_record_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.legal_entity_record
    ADD CONSTRAINT legal_entity_record_pkey PRIMARY KEY (gr_legal_entity_id);


--
-- Name: source source_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.source
    ADD CONSTRAINT source_pkey PRIMARY KEY (id);


--
-- Name: tentative_golden_record tentative_golden_record_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tentative_golden_record
    ADD CONSTRAINT tentative_golden_record_pkey PRIMARY KEY (id);


--
-- Name: tentative_legal_entity tentative_legal_entity_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tentative_legal_entity
    ADD CONSTRAINT tentative_legal_entity_pkey PRIMARY KEY (id);


--
-- Name: source uk3pganbnum82xyg852kf3qonwu; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.source
    ADD CONSTRAINT uk3pganbnum82xyg852kf3qonwu UNIQUE (source_name);


--
-- Name: legal_entity_record uk4aslgjtqock7u7t8f59jgftuw; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.legal_entity_record
    ADD CONSTRAINT uk4aslgjtqock7u7t8f59jgftuw UNIQUE (gr_inn);


--
-- Name: golden_record_external_id uk_external_id_per_source; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.golden_record_external_id
    ADD CONSTRAINT uk_external_id_per_source UNIQUE (source_id, external_id);


--
-- Name: legal_entity_external_id uk_le_external_id_per_source; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.legal_entity_external_id
    ADD CONSTRAINT uk_le_external_id_per_source UNIQUE (source_id, external_id);


--
-- Name: golden_record ukeklxwtc10dk2cvy9l5ms3npyk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.golden_record
    ADD CONSTRAINT ukeklxwtc10dk2cvy9l5ms3npyk UNIQUE (gr_pinfl);


--
-- Name: ix_gr_archive_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_gr_archive_client_id ON public.golden_record_archive USING btree (gr_client_id);


--
-- Name: ix_le_archive_record_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_le_archive_record_id ON public.legal_entity_archive USING btree (gr_legal_entity_id);


--
-- Name: ix_le_inn; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_le_inn ON public.legal_entity_record USING btree (gr_inn);


--
-- Name: ix_le_name_normalized_trgm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_le_name_normalized_trgm ON public.legal_entity_record USING gin (name_normalized public.gin_trgm_ops);


--
-- Name: ix_tentative_gr_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_tentative_gr_client_id ON public.tentative_golden_record USING btree (gr_client_id);


--
-- Name: ix_tentative_le_reason; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_tentative_le_reason ON public.tentative_legal_entity USING btree (reason);


--
-- Name: ix_tentative_le_record_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_tentative_le_record_id ON public.tentative_legal_entity USING btree (gr_legal_entity_id);


--
-- Name: ix_tentative_reason; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_tentative_reason ON public.tentative_golden_record USING btree (reason);


--
-- Name: golden_record_external_id fk18r22w8dh5r0rqbiol52j5wu1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.golden_record_external_id
    ADD CONSTRAINT fk18r22w8dh5r0rqbiol52j5wu1 FOREIGN KEY (source_id) REFERENCES public.source(id);


--
-- Name: legal_entity_external_id fk6hmrmp6y1w6otrqdi5sb5tgo7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.legal_entity_external_id
    ADD CONSTRAINT fk6hmrmp6y1w6otrqdi5sb5tgo7 FOREIGN KEY (gr_legal_entity_id) REFERENCES public.legal_entity_record(gr_legal_entity_id);


--
-- Name: legal_entity_field_meta fk7cxjdt6s1a9s212yt0xclaa0p; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.legal_entity_field_meta
    ADD CONSTRAINT fk7cxjdt6s1a9s212yt0xclaa0p FOREIGN KEY (source_id) REFERENCES public.source(id);


--
-- Name: legal_entity_field_meta fkib99busybke6ikko11ootthtx; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.legal_entity_field_meta
    ADD CONSTRAINT fkib99busybke6ikko11ootthtx FOREIGN KEY (gr_legal_entity_id) REFERENCES public.legal_entity_record(gr_legal_entity_id);


--
-- Name: golden_record_external_id fklju98li613e532ymfa1fpqtmn; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.golden_record_external_id
    ADD CONSTRAINT fklju98li613e532ymfa1fpqtmn FOREIGN KEY (gr_client_id) REFERENCES public.golden_record(gr_client_id);


--
-- Name: legal_entity_external_id fkpg00b5sspl50dxpijo41it9m; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.legal_entity_external_id
    ADD CONSTRAINT fkpg00b5sspl50dxpijo41it9m FOREIGN KEY (source_id) REFERENCES public.source(id);


--
-- Name: golden_record_field_meta fkqub07iae9winuo5qqd0qh7e0h; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.golden_record_field_meta
    ADD CONSTRAINT fkqub07iae9winuo5qqd0qh7e0h FOREIGN KEY (source_id) REFERENCES public.source(id);


--
-- Name: golden_record_field_meta fksql9tlcprkfmrr3m4e18lb3c; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.golden_record_field_meta
    ADD CONSTRAINT fksql9tlcprkfmrr3m4e18lb3c FOREIGN KEY (gr_client_id) REFERENCES public.golden_record(gr_client_id);


--
-- PostgreSQL database dump complete
--


