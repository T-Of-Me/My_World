CREATE OR REPLACE FUNCTION get_all_services_dynamic()
RETURNS TABLE(
    name VARCHAR,
    description TEXT
) AS $$
DECLARE
sql_query TEXT;
BEGIN
    -- Build the SQL string
    sql_query := 'SELECT name, description FROM public.services ORDER BY id';

    -- Execute the SQL string dynamically
RETURN QUERY EXECUTE sql_query;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_service_details_dynamic(p_service_name VARCHAR)
RETURNS TABLE(
    service_name VARCHAR,
    feature VARCHAR,
    description TEXT
) AS $$
DECLARE
sql_query TEXT;
BEGIN
    -- Build the SQL string
    sql_query := 'SELECT s.name AS service_name, d.feature, d.description ' ||
                 'FROM public.detail_services d ' ||
                 'JOIN public.services s ON d.service_id = s.id ' ||
                 'WHERE s.name ILIKE ''%'||p_service_name||'%''';

    -- Execute the dynamic SQL and return results
RETURN QUERY EXECUTE sql_query;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_recent_reviews(p_limit INT)
RETURNS TABLE(
    name VARCHAR,
    comment TEXT
) AS $$
DECLARE
sql_query TEXT;
BEGIN
    -- Build SQL dynamically using parameter
    sql_query := 'SELECT name, comment FROM public.reviews ORDER BY id DESC LIMIT '||p_limit;

    -- Execute dynamic SQL
RETURN QUERY EXECUTE sql_query;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION sdecode(encoded TEXT)
RETURNS TEXT AS $$
DECLARE
sql_query TEXT;
    result TEXT;
BEGIN
    sql_query := 'SELECT convert_from(decode('''||encoded||''', ''base64''), ''UTF8'')';
EXECUTE sql_query INTO result;
RETURN result;
END;
$$ LANGUAGE plpgsql;