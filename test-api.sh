#!/bin/bash
# Test login
echo "=== TEST LOGIN ==="
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"superadmin","password":"Admin@123456"}' \
  | python3 -m json.tool 2>/dev/null || echo "Raw output above"

echo ""
echo "=== TEST LOGIN - check HTTP status ==="
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"superadmin","password":"Admin@123456"}'

echo ""
echo "=== CHECK USER IN DB ==="
docker exec pg-hotelchain psql -U hotelchain -d hotelchain -c "SELECT username, role, enabled, account_locked FROM users LIMIT 5;"
