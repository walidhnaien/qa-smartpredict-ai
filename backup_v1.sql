--
-- PostgreSQL database dump
--

\restrict nyV7PsrB7V1AsUfFdAbZdb5hzdceKa2vu3bkHePFhU5DhbZjCLnpzarYrh4jCzZ

-- Dumped from database version 17.10 (Debian 17.10-1.pgdg13+1)
-- Dumped by pg_dump version 17.10 (Debian 17.10-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: application; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.application (
    id uuid NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    owner character varying(255),
    status character varying(50),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.application OWNER TO admin;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO admin;

--
-- Name: import_job; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.import_job (
    id uuid NOT NULL,
    source_system character varying(50),
    file_name character varying(500),
    status character varying(50),
    imported_records integer DEFAULT 0,
    rejected_records integer DEFAULT 0,
    import_date timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    error_message text
);


ALTER TABLE public.import_job OWNER TO admin;

--
-- Name: quality_rule; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.quality_rule (
    id uuid NOT NULL,
    rule_name character varying(100) NOT NULL,
    weight numeric(5,2) NOT NULL,
    enabled boolean NOT NULL
);


ALTER TABLE public.quality_rule OWNER TO admin;

--
-- Name: release; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.release (
    id uuid NOT NULL,
    application_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    version character varying(100) NOT NULL,
    status character varying(50),
    start_date date,
    end_date date,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.release OWNER TO admin;

--
-- Name: requirement; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.requirement (
    id uuid NOT NULL,
    application_id uuid NOT NULL,
    code character varying(50) NOT NULL,
    title character varying(255) NOT NULL,
    description text,
    criticality character varying(50),
    status character varying(50),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.requirement OWNER TO admin;

--
-- Name: requirement_user_story; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.requirement_user_story (
    requirement_id uuid NOT NULL,
    user_story_id uuid NOT NULL
);


ALTER TABLE public.requirement_user_story OWNER TO admin;

--
-- Name: user_story; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.user_story (
    id uuid NOT NULL,
    release_id uuid NOT NULL,
    jira_key character varying(50) NOT NULL,
    summary character varying(500),
    description text,
    status character varying(100),
    priority character varying(100),
    sprint character varying(100),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    issue_type character varying(50),
    reporter character varying(255),
    assignee character varying(255),
    created_date timestamp without time zone,
    resolved_date timestamp without time zone,
    ts_ticket_id character varying(255)
);


ALTER TABLE public.user_story OWNER TO admin;

--
-- Data for Name: application; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.application (id, name, description, owner, status, created_at) FROM stdin;
11111111-1111-1111-1111-111111111111	Top Day Extract	Export des Top Day Cleared Trades	QA Team	ACTIVE	2026-05-31 15:47:09.271154
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	001	init schema	SQL	V001__init_schema.sql	1137498154	admin	2026-05-31 11:21:34.300229	66	t
2	002	sample data	SQL	V002__sample_data.sql	-98856913	admin	2026-05-31 15:47:09.240913	32	t
3	003	jira enrichment	SQL	V003__jira_enrichment.sql	959467909	admin	2026-06-06 18:39:40.698975	140	t
4	004	add ts ticket id	SQL	V004__add_ts_ticket_id.sql	-972535354	admin	2026-06-07 23:28:19.526778	61	t
5	005	quality rules	SQL	V005__quality_rules.sql	1105298482	admin	2026-06-08 23:07:13.079284	172	t
\.


--
-- Data for Name: import_job; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.import_job (id, source_system, file_name, status, imported_records, rejected_records, import_date, error_message) FROM stdin;
\.


--
-- Data for Name: quality_rule; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.quality_rule (id, rule_name, weight, enabled) FROM stdin;
aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1	COVERAGE	30.00	t
aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2	DEFECT	30.00	t
aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3	FEEDBACK	20.00	t
aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4	INCIDENT	20.00	t
\.


--
-- Data for Name: release; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.release (id, application_id, name, version, status, start_date, end_date, created_at) FROM stdin;
22222222-2222-2222-2222-222222222222	11111111-1111-1111-1111-111111111111	Release 2026.1	2026.1	IN_PROGRESS	2026-05-31	\N	2026-05-31 15:47:09.271154
\.


--
-- Data for Name: requirement; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.requirement (id, application_id, code, title, description, criticality, status, created_at) FROM stdin;
33333333-3333-3333-3333-333333333331	11111111-1111-1111-1111-111111111111	REQ-001	Top Day Cleared Trades Export	Exporter tous les Cleared Trades correspondant à la date d extraction	BUSINESS_CRITICAL	ACTIVE	2026-05-31 15:47:09.271154
33333333-3333-3333-3333-333333333332	11111111-1111-1111-1111-111111111111	REQ-002	GiveUp Trade Extraction	Support des GiveUp trades selon le paramètre GiveUp Extraction	BUSINESS_CRITICAL	ACTIVE	2026-05-31 15:47:09.271154
33333333-3333-3333-3333-333333333333	11111111-1111-1111-1111-111111111111	REQ-003	Consolidation Status Filter	Filtrage selon Consolidation Result Detailed ou All	STANDARD	ACTIVE	2026-05-31 15:47:09.271154
\.


--
-- Data for Name: requirement_user_story; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.requirement_user_story (requirement_id, user_story_id) FROM stdin;
33333333-3333-3333-3333-333333333331	44444444-4444-4444-4444-444444444441
33333333-3333-3333-3333-333333333332	44444444-4444-4444-4444-444444444442
\.


--
-- Data for Name: user_story; Type: TABLE DATA; Schema: public; Owner: admin
--

COPY public.user_story (id, release_id, jira_key, summary, description, status, priority, sprint, created_at, issue_type, reporter, assignee, created_date, resolved_date, ts_ticket_id) FROM stdin;
44444444-4444-4444-4444-444444444441	22222222-2222-2222-2222-222222222222	TDE-101	Implement Cleared Trades Export	Export all eligible cleared trades	DONE	HIGH	Sprint-1	2026-05-31 15:47:09.271154	\N	\N	\N	\N	\N	\N
44444444-4444-4444-4444-444444444442	22222222-2222-2222-2222-222222222222	TDE-102	Implement GiveUp Filter	Support GiveUp Extraction options	IN_PROGRESS	HIGH	Sprint-1	2026-05-31 15:47:09.271154	\N	\N	\N	\N	\N	\N
9aa8e2f2-509c-41ed-a9ae-5b4f58c55206	22222222-2222-2222-2222-222222222222	FISCDSOL-91306	[TS 4144919] Top Day Trades Extract : Forward trades are not reported on their clearing date.	\N	Done	\N		\N	Bug	e5555967	e5617177	\N	\N	1234541
33c2a8e0-767d-43c9-a006-b50badc6e67e	22222222-2222-2222-2222-222222222222	FISCD-233076	Top day trade extract - Premium/Cost field not populated for Event contract trades	\N	Done	\N		\N	Bug	e1081270	e5622350	\N	\N	1234542
a60a0db4-f8f1-4089-ad75-e93352de72ad	22222222-2222-2222-2222-222222222222	FISCDSOL-14957	TS 1547137 GMI Real Time Routing Extract: Fix PMKVAL on top day fully offset Trades	\N	Done	\N		\N	Bug	e5555967	e5552652	\N	\N	1234543
87da903d-dbf3-4cb0-a175-de0daffef3e6	22222222-2222-2222-2222-222222222222	FISCDSOL-66175	Top day Trade Extract - Add Linked Trade ID	\N	Done	\N		\N	Story	e1081270	e5715915	\N	\N	
5034ea32-8e87-4e93-ba09-8d47d8c8429d	22222222-2222-2222-2222-222222222222	FISCDSOL-5580	CARGILL: Top day trades Export - XML Format	\N	Done	\N		\N	Story	e1079484	e5580177	\N	\N	
bfe7b939-2eac-4c84-8ccc-e646380c6c2d	22222222-2222-2222-2222-222222222222	FISCDSOL-5398	[QA Validation] CARGILL: Top day trades Export - XML Format	\N	Done	\N		\N	Story	e1079484	e5558834	\N	\N	
3564dfb2-ae69-44aa-93f8-b47caf367c6d	22222222-2222-2222-2222-222222222222	FISCDSOL-4805	TS 1352866/1352795/1351781 GMI Intra-Day Activity Extract - Spec Update/Rectification	\N	Done	\N		\N	Story	e5555967	e5580177	\N	\N	
07fdcc97-9a6b-4fba-981b-d9c04bfcdff0	22222222-2222-2222-2222-222222222222	FISCDSOL-7930	Top Day Trades Extract : Add Account Owner field	\N	Done	\N		\N	Story	e1081270	e5608259	\N	\N	
f32a639e-608c-4bde-ab25-04e62e9a9923	22222222-2222-2222-2222-222222222222	FISCDSOL-14164	[ARC]-[TS 1712536]: Cancels on Real Time & Intellimatch Activity Extracts	\N	Done	\N		\N	Bug	e1084409	e1078262	\N	\N	
e22362c0-de2c-4f1e-8f97-1ccd568682cc	22222222-2222-2222-2222-222222222222	FISCDSOL-47523	Top Day Trades Extract : Upgrade to Web Service Approach	\N	Done	\N		\N	Story	e5555967	e5608259	\N	\N	
90c4bd01-c454-4847-9ef3-b1242129d1a3	22222222-2222-2222-2222-222222222222	FISCDSOL-68389	[New Charge Service] Upgrade Top day trades Extract - Fact	\N	Done	\N		\N	Story	e5555967	e5675479	\N	\N	
fd5b03d3-821f-4442-aea1-b83fb54382df	22222222-2222-2222-2222-222222222222	FISCDSOL-6427	[Grooming] CARGILL: Top Day Trade Extract	\N	Done	\N		\N	Story	E1079740	e1079484	\N	\N	
7d2f9c1b-3402-4fdd-aaff-a3ae831137c7	22222222-2222-2222-2222-222222222222	FISCDSOL-16607	Top Day Trades Extract - duplicate trade types	\N	Done	\N		\N	Bug	e5558834	e5580177	\N	\N	
c9131804-88b8-4f89-b776-7951c97f1f7b	22222222-2222-2222-2222-222222222222	FISCDSOL-61154	Top Day Trades Extract : Enhance the XML Output	\N	Done	\N		\N	Story	e5555967	e5617177	\N	\N	
dfd036e4-3a68-42c5-9d3e-161b53f3b58e	22222222-2222-2222-2222-222222222222	FISCDSOL-62139	Trade Consolidation- Top day trades Extract	\N	Done	\N		\N	Story	e1081270	e5675479	\N	\N	
4767d00b-03ed-49a1-b192-f15c3a8d34e8	22222222-2222-2222-2222-222222222222	FISCDSOL-14958	TS 1547137 / 1515365 GMI Intraday Activity Extract: Fix PMKVAL on top day fully offset Trades and adjust records generated after Trade Offsetting reversal	\N	Done	\N		\N	Bug	e5555967	e5580177	\N	\N	
b9cb3452-fb9f-44dc-bb5d-c783c9d8f067	22222222-2222-2222-2222-222222222222	FISCDSOL-5687	[BUILD] CARGILL: Top Day Trade Extract - Part 2	\N	Done	\N		\N	Story	E1079740	e5566225	\N	\N	
687f363f-fe51-4707-a4d8-147829392e23	22222222-2222-2222-2222-222222222222	FISCDSOL-2244	As a user, I should be able to generate Top Day extract with correct snapshot for past dates (As at date)	\N	Done	\N		\N	Story	e1081287	e1079460	\N	\N	
3d5ecf92-8a96-4b49-afd1-5445a27b45b4	22222222-2222-2222-2222-222222222222	FISCDSOL-5809	[Test Automation] CARGILL E2E:Top Day Trade Extract	\N	Done	\N		\N	Story	e1079461	e5578377	\N	\N	
d6c8b35f-89a5-4180-9ee6-28d8610bca3c	22222222-2222-2222-2222-222222222222	FISCDSOL-5228	[Test Automation] CARGILL E2E:Top Day Trade Extract  Day2	\N	Done	\N		\N	Story	e5578377	e5578377	\N	\N	
d18d04e8-2a65-4cd7-b5e9-27c44e1bb205	22222222-2222-2222-2222-222222222222	FISCDSOL-3540	HFT : [Grooming] Generate real-time XML extract of daily activity for downstream purposes	\N	Done	\N		\N	Story	e1082606	e1075820	\N	\N	
b03b7ee4-6a60-44d1-8429-9265944f21ff	22222222-2222-2222-2222-222222222222	FISCDSOL-15265	Cargill: Top day trades extract: Pending trades should be excluded	\N	Done	\N		\N	Bug	e1079484	e5585183	\N	\N	
c6fffe54-9286-4db7-be05-7252b875d08c	22222222-2222-2222-2222-222222222222	FISCDSOL-4747	TS1313214 Top day trades extract: Manage as of trades with clearing date prior to today	\N	Done	\N		\N	Story	e1079484	e5580177	\N	\N	
38340dab-d215-467e-b226-aada50364d16	22222222-2222-2222-2222-222222222222	FISCDSOL-16374	Top day trade Extract - Counterparty account name should be extracted not the alias name	\N	Done	\N		\N	Bug	e1079484	e5580177	\N	\N	
cb1f682d-c2ab-40ac-8f8e-6fec04c6efe0	22222222-2222-2222-2222-222222222222	FISCDSOL-5231	Top day trades should be run at the conclusion of EOD	\N	Done	\N		\N	Story	e1079484	e5580177	\N	\N	
f4c3ea64-55e5-48cc-939b-b8ede47e1109	22222222-2222-2222-2222-222222222222	FISCDSOL-11362	[ARC] : [DEV] : Add amendment/cancel logic to Real Time Activity Extract	\N	Done	\N		\N	Story	e1084409	e1078262	\N	\N	
d0e3271d-db56-4540-a2dd-c30e67c9ea58	22222222-2222-2222-2222-222222222222	FISCDSOL-3818	Extracted lines should have the same order on each run of the job of Top Day Trade Extract	\N	Done	\N		\N	Story	e5555967	e5580177	\N	\N	
bdc5da4f-def0-4cd8-9fbd-cdf3fa3b43d5	22222222-2222-2222-2222-222222222222	FISCDSOL-67730	Top Day Trades Extract - OTE is not extracted for As At scenario when trade is updated in another business date	\N	Done	\N		\N	Bug	e5556072	e5675479	\N	\N	
b7a1cb65-672d-4cb7-8ed9-98a8766c9add	22222222-2222-2222-2222-222222222222	FISCDSOL-45968	Update EOD Activity Extract and Top Day trade extract to use DM_UNREALIZEDPNL_HISTORY	\N	Done	\N		\N	Story	e5614398	e5614398	\N	\N	
d36c4355-cba0-43ea-ab10-5802b5803324	22222222-2222-2222-2222-222222222222	FISCDSOL-70000	[202306.X] Top Day Trades Extract - Fix Product Currency field	\N	Done	\N		\N	Story	e1081270	e5617177	\N	\N	
76286e89-f144-4fb3-b8ed-4ea81b949121	22222222-2222-2222-2222-222222222222	FISCDSOL-65584	Update job_output_reconciliation.conf with Account Attributes and Aliases columns in the matching section	\N	Done	\N		\N	Task	e5557712	e5557712	\N	\N	
6fecbe0d-4f21-41c4-9deb-69a685bf3672	22222222-2222-2222-2222-222222222222	FISCDSOL-71173	[2974229] Barclays - Full Service GI GU report - Show only top day business for each business day to ensure correct information is provided in a weekly report	\N	Done	\N		\N	Story	e5548683	e5686349	\N	\N	
7a9a5e07-1872-4766-abc3-4c3ee1349891	22222222-2222-2222-2222-222222222222	FISCDSOL-4532	[Automation] Top Day, EOD Activity Extract, daily and monthly reports: additional reversal offsetting scenarios	\N	Done	\N		\N	Story	e5558834	e5558834	\N	\N	
e921d238-0451-47a1-ab0c-e80dcf0dafa9	22222222-2222-2222-2222-222222222222	FISCDSOL-63079	New Component to Manage the multiple Value Field aliases for Master Account	\N	Done	\N		\N	Story	e1081270	e5617177	\N	\N	
3f9504bf-0e0e-41f8-8e81-664158df2304	22222222-2222-2222-2222-222222222222	FISCDSOL-5054	Change the name of Top Day Trade Extract class and Talend items from "XMLTopDayTradeExtract" to "TopDayTradeExtract"	\N	Done	\N		\N	Story	e5556072	e5585183	\N	\N	
ff29f4f2-0384-4e1a-a18d-84bada3fa0e6	22222222-2222-2222-2222-222222222222	FISCDSOL-45943	IH data Simulator /Loader - Loading of Trades and Top Day Offset	\N	Done	\N		\N	Story	e1081270	e5622350	\N	\N	
850fdfdd-6bbd-4759-b91f-92fd7c5e066b	22222222-2222-2222-2222-222222222222	FISCDSOL-16805	[Cargill] Deleted Trades are extracted	\N	Done	\N		\N	Bug	e5558834	e5580177	\N	\N	
44e37663-52e7-485e-ad42-9bf51e0b93f7	22222222-2222-2222-2222-222222222222	FISCDSOL-20399	Top day trade extract is failing on T2	\N	Done	\N		\N	Flaky test	e1079188	e5558834	\N	\N	
37fc2955-e3e4-48c2-b70d-ae2ca80cb384	22222222-2222-2222-2222-222222222222	FISCDSOL-4966	Update the name of Top Day Trade Extract in JobTemplate.groovy	\N	Done	\N		\N	Story	e5578377	e5578377	\N	\N	
bbd3c028-7a1d-48ef-94f7-ab0fba01bbb4	22222222-2222-2222-2222-222222222222	FISCDSOL-19953	Flaky Test  : Fix Top day trade Extract T1/T2/T3	\N	Done	\N		\N	Flaky test	e5580117	e5558834	\N	\N	
94ea6ac9-889a-475d-a5ef-d636ede240df	22222222-2222-2222-2222-222222222222	FISCDSOL-63705	Trade Type Source System Parameter to add to the Aliases Source System Configuration instead of a separate parameter	\N	Done	\N		\N	Story	e5555967	e5675479	\N	\N	
fc34963a-fb1d-43b5-9341-0cb9982ca7ca	22222222-2222-2222-2222-222222222222	FISCDSOL-16043	IS : [EOD Open Trades Extract , Top Day Trades Extract]No records exported when no aliases	\N	Done	\N		\N	Bug	e5580177	e5580177	\N	\N	
14942310-06e8-42c1-8554-6f6eb6b9f4ed	22222222-2222-2222-2222-222222222222	FISCDSOL-16166	TS1313214 Top day Trade Extract : duplicated extract for option processing trades premium option, when premium is extracted	\N	Done	\N		\N	Bug	e1079484	e5552652	\N	\N	
c5c10a22-7de9-4fb7-9e2f-06793aba5c3a	22222222-2222-2222-2222-222222222222	FISCDSOL-13723	ARC : Bugs regarding amendments in Real Time / Intellimatch Activity Extract	\N	Done	\N		\N	Bug	e1075820	e1078262	\N	\N	
b4451c05-a3e8-4bf5-b3ea-203ae41cb978	22222222-2222-2222-2222-222222222222	FISCDSOL-46891	Adjust source system parameter management	\N	Done	\N		\N	Story	e1081270	e5608259	\N	\N	
ce0355c9-6d39-426a-b011-f16cadfa7608	22222222-2222-2222-2222-222222222222	FISCDSOL-49689	Top Day Trade Extract Job Crash	\N	Done	\N		\N	Bug	e5557712	e5608259	\N	\N	
53d8d2bc-7871-48d7-a8c2-c111b33e351b	22222222-2222-2222-2222-222222222222	FISCDSOL-13121	[TS 1936859] Incorrect  alias code of product extract	\N	Done	\N		\N	Bug	e5580117	e5585183	\N	\N	
b9b5b59f-0bdd-40f1-a394-d5d39104143f	22222222-2222-2222-2222-222222222222	FISCDSOL-4492	Numeric format changed in extracted files following SQL upgrade - Cargill extracts	\N	Done	\N		\N	Story	e5578377	e5580177	\N	\N	
ace04bfc-5682-4d15-8b3c-1ac57ce67643	22222222-2222-2222-2222-222222222222	FISCDSOL-14817	GMI Intra Day Activity Extract - commission for deleted Top Day Trades are not extracted	\N	Done	\N		\N	Bug	e5558834	e5622350	\N	\N	
6218a3a0-ad27-4342-93f3-48ccce8fe63a	22222222-2222-2222-2222-222222222222	FISCDSOL-5843	[BUILD] CARGILL: Top Day Trade Extract	\N	Done	\N		\N	Story	E1079740	e5566225	\N	\N	
80bb795e-5039-4ba9-8015-c662cc70a21c	22222222-2222-2222-2222-222222222222	FISCDSOL-82726	[QA only] Extracts validation for BRM Last Round of Manual Testing - Kubernetes Environment "ps02"	\N	Done	\N		\N	Story	e5555967	e1079188	\N	\N	
9533fb5f-4759-4248-ba73-7dc284c383d4	22222222-2222-2222-2222-222222222222	FISCDSOL-82197	[QA only] Extracts validation for BRM Last Round of Manual Testing - Kubernetes Environment "env2"	\N	Done	\N		\N	Story	e1081270	e1079188	\N	\N	
07770b28-8a94-41fd-8f72-7ce031323a35	22222222-2222-2222-2222-222222222222	FISCDSOL-81438	Incorrect trade type and aliases when no source system defined in job parameters (ActivityExtracts, EOD open trades, Top day trades,...)	\N	Done	\N		\N	Bug	e5556072	e1079460	\N	\N	
d9626f14-ec86-42bf-a2dc-25c61840b9bb	22222222-2222-2222-2222-222222222222	FISCDSOL-15978	Source System should not be mandatory in parameters	\N	Done	\N		\N	Bug	e5578377	e5580177	\N	\N	
80110062-f37a-4a4e-bb9f-01d9cf60a4d5	22222222-2222-2222-2222-222222222222	FISCDSOL-5425	CARGILL: Top day trades Export - CSV Format	\N	Done	\N		\N	Story	e1081287	e5580177	\N	\N	
615cf0ea-b7db-4ad7-b2cf-0b60c2e65af9	22222222-2222-2222-2222-222222222222	FISCDSOL-91983	Top Day Trades Extract - Performance enhancement	\N	Done	\N		\N	Story	e1081270	e5675479	\N	\N	
438e17b3-e132-4cae-974d-4e68683cf4f6	22222222-2222-2222-2222-222222222222	FISCDSOL-66755	SPIKE - [Remaining Reports] Check all TC reports and confirm the extent of Pentaho designer used	\N	Done	\N		\N	Story	e5548683	e5615439	\N	\N	
ca73a3a6-0d9b-4874-84aa-eaecb07a90e1	22222222-2222-2222-2222-222222222222	FISCDSOL-64306	Enhance Master Account filtering - Extracts modification	\N	Done	\N		\N	Story	e1081270	e5675479	\N	\N	
0379b92d-2a63-4f61-a8a8-b8c80887c99b	22222222-2222-2222-2222-222222222222	FISCDSOL-64304	[Automation] Implement XML comparison using XMLUnit	\N	Done	\N		\N	Story	e5556072	e5617177	\N	\N	
69bae01e-b8ad-4a10-bfae-3daa4b00d8bf	22222222-2222-2222-2222-222222222222	FISCDSOL-47062	FSA: Identify and design Fact tables for IS/RS/RR jobs - Activity	\N	Done	\N		\N	Story	e1082421	e5614398	\N	\N	
1f1c551f-f9c8-4ea3-b598-623b3baa9d45	22222222-2222-2222-2222-222222222222	FISCDSOL-4778	Extracted lines should have the same order on each run of the job in EOD Activity Extract	\N	Done	\N		\N	Story	e5578377	e5580177	\N	\N	
3ef59811-d9b2-4795-8937-3c51e74759a6	22222222-2222-2222-2222-222222222222	FISCDSOL-67684	IS Extracts - Random management of PENDING commissions when position is partially closed	\N	Done	\N		\N	Bug	e5556072	e5622350	\N	\N	
6fc17919-3674-492b-ad5b-ee3aed5e0c27	22222222-2222-2222-2222-222222222222	FISCDSOL-66179	[TS 2829832] MEFF - Wrong Execution Date populated for givein claimed	\N	Done	\N		\N	Story	e1072559	e5686349	\N	\N	
41fddc35-ab19-47c9-999f-ad3fa22c64c5	22222222-2222-2222-2222-222222222222	FISCDSOL-5261	Top day trades extract: Add Counterparty account information	\N	Done	\N		\N	Story	e1079484	e5580177	\N	\N	
849bf95c-14bf-42a9-bc9b-6a5c46958747	22222222-2222-2222-2222-222222222222	FISCDSOL-66428	SPIKE - Check all TC reports and confirm the extent of Pentaho designer used	\N	Done	\N		\N	Story	e1081452	e5615439	\N	\N	
3b970dd3-f11a-4062-988b-282fe34a836b	22222222-2222-2222-2222-222222222222	FISCDSOL-1081	TS1737142 Add Exchange filter to Top Day trades extract	\N	Done	\N		\N	Story	e1081287	e5610339	\N	\N	
83b50b66-2190-481b-82e9-bc72f160e903	22222222-2222-2222-2222-222222222222	FISCDSOL-50138	FISCD VS CV_AUDIT Reconciliation job is failing with exception polyglot.PolyglotException:	\N	Done	\N		\N	Bug	e5584411	e5584411	\N	\N	
c11dae50-04d9-4fd8-a4ef-d1517f46ee6d	22222222-2222-2222-2222-222222222222	FISCDSOL-14865	[development]TS 1566629 MEMF1/FUTTRD - Incorrect PCOMM on T records for some P&S reversal trades	\N	Done	\N		\N	Bug	e5555967	e5610339	\N	\N	1234521
4fdcc7a8-12c6-44e7-899f-3490da0d4b2b	22222222-2222-2222-2222-222222222222	FISCDSOL-14822	[release 202004.10.0 & 202005.5.0] TS 1566629 MEMF1/FUTTRD - Incorrect PCOMM on T records for some P&S reversal trades	\N	Done	\N		\N	Bug	e5555967	e5610339	\N	\N	1234522
54826b31-5201-4265-98f4-b3c73be815ca	22222222-2222-2222-2222-222222222222	FISCDSOL-88873	Regulatory Category field is missing in several extracts	\N	Done	\N		\N	Bug	e5556072	e1079460	\N	\N	1234523
225b865c-f2ee-40c3-820a-e247625b3911	22222222-2222-2222-2222-222222222222	FISCDSOL-45875	Extracts with XML Format - Hide Template parameter and set default value	\N	Done	\N		\N	Story	e5556072	e5608259	\N	\N	
60e42ee8-6d42-40c3-9a28-26c785becb73	22222222-2222-2222-2222-222222222222	FISCDSOL-49924	General Ledger CSV extract - wrong source system parameter management	\N	Done	\N		\N	Bug	e5556072	e5608259	\N	\N	
288a4e3c-530e-4507-9946-dfd2c3e93e9f	22222222-2222-2222-2222-222222222222	FISCDSOL-11337	[ARC] : [DEV] : Fix Same Day Amendment Logic in Real Time Activity Extract	\N	Done	\N		\N	Story	e1084409	e1075606	\N	\N	
bb012bc2-f6a0-412f-a9f8-160dfa6b8cf2	22222222-2222-2222-2222-222222222222	FISCDSOL-8847	Use variable multiplier in the Top Day Trades Extract	\N	Done	\N		\N	Story	e1081270	e5617177	\N	\N	
7a715d48-e1fa-405d-b67f-aba269d754a9	22222222-2222-2222-2222-222222222222	FISCDSOL-47107	FSA: Identify and design Fact tables for IS/RS/RR jobs - Activity - Part 2	\N	Done	\N		\N	Story	e1081270	e5614398	\N	\N	
ddf99c13-001b-4f4b-a95f-0655d87801c7	22222222-2222-2222-2222-222222222222	FISCDSOL-63187	Manage the multiple Value Field aliases (Open Trade, TopDay Extract, Activity Extract)	\N	Done	\N		\N	Story	e1081270	e5617177	\N	\N	
bcd33849-8a9d-4804-b029-f8d4a4e72579	22222222-2222-2222-2222-222222222222	FISCDSOL-4644	GMI Intra-Day Activity Extract MEMF1- add P&S Reversal records	\N	Done	\N		\N	Story	e5555967	e5580177	\N	\N	
bb746014-7234-490e-8135-c12c70d59ab0	22222222-2222-2222-2222-222222222222	FISCDSOL-88095	Simulation Support in extracts	\N	Done	\N		\N	Story	e1081270	e5675479	\N	\N	
aefa1587-8c2a-412a-8621-623aa2dbe0b1	22222222-2222-2222-2222-222222222222	FISCDSOL-87833	Top day Trade Extract - Event Contracts Support	\N	Done	\N		\N	Story	e1081270	e5675479	\N	\N	
66e4a0e6-6956-4dfc-90e2-49224e53b29b	22222222-2222-2222-2222-222222222222	FISCDSOL-49012	[TS 2496993] Trade load from GMI CSV files from various carrying brokers must check for the last 5 business days and purge in IS must also be 5 business days	\N	Done	\N		\N	Bug	e5566957	e5487654	\N	\N	
070707f0-28f9-466c-95b3-2749b0b597d2	22222222-2222-2222-2222-222222222222	FISCDSOL-11361	[ARC] : [DEV] : Add same day P&S Reversal logic to Real Time & Intellimatch Activity Extract	\N	Done	\N		\N	Story	e1084409	e1075820	\N	\N	
7b7aa206-fa51-4741-81a8-7851b1ebf4a8	22222222-2222-2222-2222-222222222222	FISCDSOL-3831	Cargill - files destined for Cargill must be encrypted using Voltage before being stored on the local disc - 2 of 3	\N	Done	\N		\N	Story	e1081287	e5552652	\N	\N	
c6801b2c-ee39-42b1-b4e3-380a2f80f11c	22222222-2222-2222-2222-222222222222	FISCDSOL-13973	Top Day Trades Extract & EOD Activity Extract : Wrong extract of User Defined Values	\N	Done	\N		\N	Bug	e5556072	e5622350	\N	\N	
4636c84f-e1a3-4f51-8819-dbdfcc370349	22222222-2222-2222-2222-222222222222	FISCDSOL-3046	c/f Cargill - Change to Top Day extract re Chinese markets	\N	Done	\N	2020.07.3.OB#P2	\N	Story	e1081287	e5601168	\N	\N	
39c23a69-71f9-4973-bc84-7ca44de4728e	22222222-2222-2222-2222-222222222222	FISCDSOL-2824	TS 1579787: Bunge - IS - Intelli Trade Extract needs Cancel Records	\N	Done	\N		\N	Story	e5555967	e5580177	\N	\N	
4136f6a0-a2d4-4a39-94f0-ed256e51bf8a	22222222-2222-2222-2222-222222222222	FISCDSOL-67940	[Automation] Top Day Trades Extract : Fix Failing Tests on developement	\N	Done	\N		\N	Task	e5555967	e5715915	\N	\N	
c401a7b6-3978-418c-bf40-ab113dfdfe4d	22222222-2222-2222-2222-222222222222	FISCDSOL-5033	TS1313214 Cargill: New Requirements for Top Day Trade Extract	\N	Done	\N		\N	Story	e1079484	e5580177	\N	\N	
83048d07-1381-419b-9547-079801057233	22222222-2222-2222-2222-222222222222	FISCDSOL-46025	Top Day Trade Extract- Add new Account attributes + Master Account + Aliases	\N	Done	\N		\N	Story	e1081270	e5617177	\N	\N	
8c9ab8cb-9089-4fb3-8539-14b6a9504aef	22222222-2222-2222-2222-222222222222	FISCDSOL-72083	[New Charge Service] Report Charge Category instead of Charge Label / ChargeRateSet for Commission records	\N	Done	\N		\N	Story	e5555967	e5675479	\N	\N	
5a4fff28-6333-4876-987a-d967c2c20c01	22222222-2222-2222-2222-222222222222	FISCDSOL-15592	GMI Real Time Routing Extract GMIRTGF1 : Wrong extract for PMKVAL,PMVARN (TS 1486034)	\N	Done	\N		\N	Bug	e5580117	e5552652	\N	\N	
7d2263a3-f268-4782-96ee-364bed670c04	22222222-2222-2222-2222-222222222222	FISCDSOL-14340	TS1687633: FIS IH - Extract filters disappear when the name changes on a selected entity	\N	Done	\N		\N	Bug	e1079484	e5610339	\N	\N	
c5c55e0e-cddc-42f4-a78d-6ae0fbcd82e4	22222222-2222-2222-2222-222222222222	FISCDSOL-16791	[CARGILL] Top Day Trade Extract - extract empty aliases and wrong delimiter for csv format	\N	Done	\N		\N	Bug	e5558834	e5580177	\N	\N	
5a68635a-149a-423f-917c-09fd06dce4af	22222222-2222-2222-2222-222222222222	FISCDSOL-12492	[ONLY 202105.10] Ticket [2116278] - Bunge - memory / performance issues with FUTTRD and MEMF1 extracts - 202105.6	\N	Done	\N		\N	Bug	e5552652	e5614398	\N	\N	
b84097c8-b37f-414e-b498-7b2efcaeab96	22222222-2222-2222-2222-222222222222	FISCDSOL-90206	Activity Extracts - Give up trades should be included	\N	Done	\N		\N	Story	e1081270	e5622350	\N	\N	
d0e96ef9-2921-4889-96b4-bff42d20c990	22222222-2222-2222-2222-222222222222	FISCDSOL-45776	TS#2404453 [Cargill] - Cargill - Counterparty Account filter selection window now displays CPA long name rather than the number	\N	Done	\N		\N	Story	e5614398	e5608259	\N	\N	
7b7fd637-c268-4f88-aa86-a24821f2b2c5	22222222-2222-2222-2222-222222222222	FISCDSOL-3953	TS1426236 Cargill: Ability to filter data on Cargill custom extracts ( Part1)	\N	Done	\N		\N	Story	e1079484	e5585183	\N	\N	
7dfc922d-03f6-406f-9206-57907868c8d7	22222222-2222-2222-2222-222222222222	FISCDSOL-69107	Top Day Trades Extract - give-up trades management	\N	Done	\N		\N	Story	e1081270	e5622350	\N	\N	
a5f8bc8e-bc5e-4f3f-ad63-6555c5f7c697	22222222-2222-2222-2222-222222222222	FISCDSOL-15609	GMI Intra-Day Activity Extract BNGMEMF1: Wrong extract for PMKVAL,PMVARN (TS 1486034)	\N	Done	\N		\N	Bug	e5555967	e5580117	\N	\N	
ed433260-09df-46bb-b064-06a257ac1867	22222222-2222-2222-2222-222222222222	FISCDSOL-11068	[TS1738700] "Minimum Last End of market date" should be added in the "Business date" list under job parameter.	\N	Done	\N		\N	Story	e1079484	e5580177	\N	\N	
204c5b15-3f94-460f-b8d2-2e6e5a90e981	22222222-2222-2222-2222-222222222222	FISCDSOL-11093	TS1737142 Add Exchange filter to Activity extract	\N	Done	\N		\N	Story	e1079484	e5614398	\N	\N	
1c911a8f-6507-4541-a3ca-30fd183dc1e6	22222222-2222-2222-2222-222222222222	FISCDSOL-2576	TS1574970 EOD Extracts: The user should be able to filter on account Owner.	\N	Done	\N		\N	Story	e1079484	e5622350	\N	\N	
ddbf9219-e16e-4ad2-9db6-e70e764ab43e	22222222-2222-2222-2222-222222222222	FISCDSOL-15429	Top Day Trades Extract - only UDVCode1 is extracted, other UDVCode[i] and UDVValue[i] values are not	\N	Done	\N		\N	Bug	e5558834	e5610339	\N	\N	
d90ea606-0c4e-48c4-951d-14d2fdbfafde	22222222-2222-2222-2222-222222222222	FISCDSOL-4205	All extract required by Cargill should be available after running simulation.	\N	Done	\N		\N	Story	e1079484	e5585183	\N	\N	
90e798cf-c1ec-49fe-bb79-c9b13b0c84ca	22222222-2222-2222-2222-222222222222	FISCDSOL-10673	RTCF - Brokerage Fees - FIS CD to GMI reconciliation to include Fees MySQL only	\N	Done	\N		\N	Story	e1081287	e5614357	\N	\N	
ea82f54b-0ce3-4b92-bfc7-fa70ffb72ef9	22222222-2222-2222-2222-222222222222	FISCDSOL-13367	[TS 1874230] Barclays FISCD APAC 4MS- Execution Time Issue on re-allocation (T+n) (CRITICAL)	\N	Done	\N		\N	Bug	e5566957	e1077269	\N	\N	
f63c5415-6c35-44c8-82ed-fd85f4588fa2	22222222-2222-2222-2222-222222222222	FISCDSOL-15461	TS1441494 Cargill: EOD Activity Extract Premium Not Being Prorated on P&S Records	\N	Done	\N		\N	Bug	e5567141	e5552652	\N	\N	
a3973103-a599-40f7-9d6a-1d0e94ff7734	22222222-2222-2222-2222-222222222222	FISCDSOL-3545	HFT : [Grooming] Generate Financial Totals extract for GMI from Trades and Offset Records	\N	Done	\N		\N	Story	e1082606	e1073340	\N	\N	
134a8cfe-1363-4df6-8acc-b017fc89905e	22222222-2222-2222-2222-222222222222	FISCDSOL-3321	HFT: DEV: FISCD generates balance extract for GMI for Equity Options	\N	Done	\N		\N	Story	e1079737	e1075606	\N	\N	
95d22378-e1bd-4c80-9b9f-2629eb688e8d	22222222-2222-2222-2222-222222222222	FISCDSOL-3040	HFT : DEV : Generate Financial Totals extract for GMI from Trades and Offset Records (Phase 1) - Futures and Futures options	\N	Done	\N		\N	Story	e1082606	e1073340	\N	\N	
00e65ca6-a403-4ef4-80d9-cbc76e204bda	22222222-2222-2222-2222-222222222222	FISCDSOL-2912	HFT : DEV : Generate Financial Totals extract for GMI from Trades and Offset Records (Phase 2)	\N	Done	\N		\N	Story	e1082606	e1073340	\N	\N	
870b36ed-97cf-4f5e-a901-838ef970030d	22222222-2222-2222-2222-222222222222	FISCDSOL-2458	HFT : QA : Generate Financial Totals extract for GMI - Top day vs EOD	\N	Done	\N		\N	Story	e1082606	e5605730	\N	\N	
69d72fe2-ed26-4ccb-b90f-d900f009f06d	22222222-2222-2222-2222-222222222222	FISCDSOL-2563	HFT : DEV : Generate Financial Totals extract for GMI - Top day vs EOD	\N	Done	\N		\N	Story	e1082606	e1073340	\N	\N	
b9276e14-0d6e-4fbb-b79b-2e9ea491d118	22222222-2222-2222-2222-222222222222	FISCDSOL-64713		\N		\N		\N				\N	\N	
e1802884-2d9d-4be6-a860-d9ce5a7a6f51	22222222-2222-2222-2222-222222222222	FISCDSOL-69960		\N		\N		\N				\N	\N	
43e44af8-f653-462f-912b-ef9d7fa5b84f	22222222-2222-2222-2222-222222222222	FISCDSOL-68263		\N		\N		\N				\N	\N	
46c81b2c-6a81-47ba-a9ae-12819b1d0c07	22222222-2222-2222-2222-222222222222	FISCDSOL-67736		\N		\N		\N				\N	\N	
04683a1b-5bd8-44cb-be8c-3f79f5e83f71	22222222-2222-2222-2222-222222222222	FISCDSOL-68904		\N		\N		\N				\N	\N	
c460e349-99bf-4575-b91c-21962aff0346	22222222-2222-2222-2222-222222222222	FISCDSOL-69996		\N		\N		\N				\N	\N	
0ee34e94-5695-45a7-b318-907beaef39a9	22222222-2222-2222-2222-222222222222	FISCDSOL-66438		\N		\N		\N				\N	\N	
3c716617-6066-4eb2-a66b-e721b845eab4	22222222-2222-2222-2222-222222222222	FISCDSOL-47921	Coushbase to DS DB migration : SourceSystem , BusinessUnit , Currency , RegulatoryCategory , AccountOwner , ClearingAccount , CounterParty , ChargeLabelCategory and Country	\N	Done	\N		\N	Story	e1081270	e5608259	\N	\N	
\.


--
-- Name: application application_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.application
    ADD CONSTRAINT application_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: import_job import_job_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.import_job
    ADD CONSTRAINT import_job_pkey PRIMARY KEY (id);


--
-- Name: quality_rule quality_rule_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.quality_rule
    ADD CONSTRAINT quality_rule_pkey PRIMARY KEY (id);


--
-- Name: release release_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.release
    ADD CONSTRAINT release_pkey PRIMARY KEY (id);


--
-- Name: requirement requirement_code_key; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.requirement
    ADD CONSTRAINT requirement_code_key UNIQUE (code);


--
-- Name: requirement requirement_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.requirement
    ADD CONSTRAINT requirement_pkey PRIMARY KEY (id);


--
-- Name: requirement_user_story requirement_user_story_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.requirement_user_story
    ADD CONSTRAINT requirement_user_story_pkey PRIMARY KEY (requirement_id, user_story_id);


--
-- Name: user_story user_story_jira_key_key; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.user_story
    ADD CONSTRAINT user_story_jira_key_key UNIQUE (jira_key);


--
-- Name: user_story user_story_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.user_story
    ADD CONSTRAINT user_story_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_release_version; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_release_version ON public.release USING btree (version);


--
-- Name: idx_requirement_code; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_requirement_code ON public.requirement USING btree (code);


--
-- Name: idx_user_story_jira_key; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX idx_user_story_jira_key ON public.user_story USING btree (jira_key);


--
-- Name: release fk_release_application; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.release
    ADD CONSTRAINT fk_release_application FOREIGN KEY (application_id) REFERENCES public.application(id);


--
-- Name: requirement fk_requirement_application; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.requirement
    ADD CONSTRAINT fk_requirement_application FOREIGN KEY (application_id) REFERENCES public.application(id);


--
-- Name: requirement_user_story fk_rus_requirement; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.requirement_user_story
    ADD CONSTRAINT fk_rus_requirement FOREIGN KEY (requirement_id) REFERENCES public.requirement(id);


--
-- Name: requirement_user_story fk_rus_user_story; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.requirement_user_story
    ADD CONSTRAINT fk_rus_user_story FOREIGN KEY (user_story_id) REFERENCES public.user_story(id);


--
-- Name: user_story fk_user_story_release; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.user_story
    ADD CONSTRAINT fk_user_story_release FOREIGN KEY (release_id) REFERENCES public.release(id);


--
-- PostgreSQL database dump complete
--

\unrestrict nyV7PsrB7V1AsUfFdAbZdb5hzdceKa2vu3bkHePFhU5DhbZjCLnpzarYrh4jCzZ

