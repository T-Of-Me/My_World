CREATE TABLE IF NOT EXISTS public.services (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL
    );
CREATE TABLE if not exists public.reviews (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    comment TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
    );
CREATE TABLE if not exists public.contact (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    address TEXT
    );
CREATE TABLE IF NOT EXISTS public.detail_services (
    id SERIAL PRIMARY KEY,
    service_id INT NOT NULL REFERENCES public.services(id) ON DELETE CASCADE,
    feature VARCHAR(255) NOT NULL,
    description TEXT NOT NULL
    );

INSERT INTO public.services (name, description) VALUES
    ('Custom Software Development',
     'We build scalable, maintainable, and secure web and mobile applications tailored to your business needs.'),
    ('System Architecture Design',
     'Designing high-performance and resilient system architectures to ensure scalability and reliability.'),
    ('Cloud & DevOps Consulting',
     'We provide migration, CI/CD, and cloud optimization services for AWS, Azure, and GCP.'),
    ('Technical Support & Maintenance',
     'Ongoing technical assistance and performance monitoring for enterprise systems.'),
    ('UI/UX Design Services',
     'Creating intuitive user experiences and modern interface designs to elevate your digital presence.');

INSERT INTO public.reviews (name, comment) VALUES
   ('Alice Nguyen', 'DevSpark delivered our app on time with outstanding quality. Highly professional team!'),
   ('David Tran', 'Their DevOps consulting helped us reduce deployment times by 70%. Highly recommended.'),
   ('Hoang Le', 'Great collaboration and transparent process. Our system runs smoother than ever.');


INSERT INTO public.contact (email, phone, address) VALUES
    ('contact@devspark.io', '+84 912 345 678', '12A Innovation Street, District 1, Ho Chi Minh City, Vietnam');

INSERT INTO public.detail_services (service_id, feature, description) VALUES
    (1, 'Mobile App Development', 'We create native and cross-platform mobile applications.'),
    (1, 'Web App Development', 'We develop responsive and scalable web applications.'),
    (2, 'High Availability Architecture', 'Design systems to be fault-tolerant and scalable.'),
    (2, 'Microservices', 'Implement microservice architecture for modular design.'),
    (3, 'Cloud Migration', 'Migrate existing systems to AWS, Azure, or GCP.'),
    (3, 'CI/CD Setup', 'Automate deployment pipelines for faster delivery.');