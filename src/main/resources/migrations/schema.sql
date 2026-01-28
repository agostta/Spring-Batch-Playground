create table if not exists transactions (
    id bigserial primary key,
    external_id varchar(64) not null,
    customer_id varchar(64) not null,
    amount_in_cents bigint not null,
    currency varchar(3) not null,
    fee_in_cents bigint not null,
    transaction_type varchar(16) not null,
    created_at timestamptz not null
);

create index if not exists idx_transactions_external_id on transactions (external_id);

create table if not exists customers (
    id bigserial primary key,
    name text not null,
    zipcode varchar(8) not null,
    street text,
    neighborhood text,
    city text,
    state varchar(2),
    address_status varchar(20) not null default 'PENDING',
    updated_at timestamp
);

create index if not exists idx_customers_address_status on customers (address_status);
create index if not exists idx_customers_zipcode on customers (zipcode);

insert into customers (name, zipcode)
values
    ('Raquel Silva', '01001000'),
    ('Marcos Agostta', '14805400'),
    ('Maria Rocha', '02013040'),
    ('Alice Pereira', '04003000'),
    ('Marcelo Souza', '30140071');
