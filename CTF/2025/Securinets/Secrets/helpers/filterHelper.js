function sanitizeInput(input) {
  return input.replace(/[^a-zA-Z0-9 _-]/g, '');
}

function isValidFilterField(field, allowedFields) {
  return allowedFields.includes(field);
}


function filterBy(table, filterBy, keyword, paramIndexStart = 1) {
  if (!filterBy || !keyword) {
    return { clause: "", params: [] };
  }

  const clause = ` WHERE ${table}."${filterBy}" LIKE $${paramIndexStart}`;
  const params = [`%${keyword}%`];

  return { clause, params };
}

module.exports = { filterBy, sanitizeInput, isValidFilterField };
