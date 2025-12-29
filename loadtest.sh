#!/usr/bin/env bash
# ============================================
# TP27 - Test de Concurrence, Verrous DB & Résilience
# Script de test de charge Bash
# ============================================
#
# Usage: ./loadtest.sh <BOOK_ID> <REQUESTS>
# Exemple: ./loadtest.sh 1 50
#
# Ce script:
# - Lance N requêtes POST /api/books/{id}/borrow en parallèle
# - Répartit les requêtes sur 3 instances (8081, 8083, 8084)
# - Compte les succès (200), conflits (409) et erreurs
#

set -euo pipefail

BOOK_ID="${1:-1}"
REQUESTS="${2:-50}"

# On répartit sur 3 instances
PORTS=(8081 8083 8084)

echo ""
echo "============================================"
echo "  LOAD TEST - TP27 Concurrence"
echo "============================================"
echo ""
echo "BookId   : $BOOK_ID"
echo "Requests : $REQUESTS"
echo "Ports    : ${PORTS[*]}"
echo ""
echo "Lancement des requêtes en parallèle..."
echo ""

tmpdir="$(mktemp -d)"
success_file="$tmpdir/success.txt"
conflict_file="$tmpdir/conflict.txt"
other_file="$tmpdir/other.txt"

touch "$success_file" "$conflict_file" "$other_file"

run_one() {
  local i="$1"
  local port="${PORTS[$((i % 3))]}"
  local url="http://localhost:${port}/api/books/${BOOK_ID}/borrow"

  # -s: silent, -o: body in file, -w: status code
  local body_file="$tmpdir/body_$i.json"
  local status
  status="$(curl -s -o "$body_file" -w "%{http_code}" -X POST "$url" --connect-timeout 10 --max-time 30 || true)"

  if [[ "$status" == "200" ]]; then
    echo "$port $status $(cat "$body_file")" >> "$success_file"
  elif [[ "$status" == "409" ]]; then
    echo "$port $status $(cat "$body_file")" >> "$conflict_file"
  else
    echo "$port $status $(cat "$body_file" 2>/dev/null || echo 'No response')" >> "$other_file"
  fi
}

pids=()
for i in $(seq 1 "$REQUESTS"); do
  run_one "$i" &
  pids+=($!)
done

for p in "${pids[@]}"; do
  wait "$p" 2>/dev/null || true
done

success_count=$(wc -l < "$success_file" | tr -d ' ')
conflict_count=$(wc -l < "$conflict_file" | tr -d ' ')
other_count=$(wc -l < "$other_file" | tr -d ' ')

echo ""
echo "============================================"
echo "  RESULTATS"
echo "============================================"
echo ""
echo "Success (200)  : $success_count"
echo "Conflict (409) : $conflict_count"
echo "Other          : $other_count"
echo ""
echo "Fichiers détails: $tmpdir"
echo " - success.txt  : appels OK (emprunt réussi)"
echo " - conflict.txt : stock épuisé (comportement normal)"
echo " - other.txt    : erreurs à diagnostiquer"
echo ""

# Afficher les derniers succès
if [[ "$success_count" -gt 0 ]]; then
  echo "--- Derniers succès ---"
  tail -3 "$success_file"
  echo ""
fi

# Afficher les erreurs s'il y en a
if [[ "$other_count" -gt 0 ]]; then
  echo "--- Erreurs (Other) ---"
  cat "$other_file"
  echo ""
fi

echo "============================================"
echo ""
