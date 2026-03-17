import json
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from django.core.serializers.json import DjangoJSONEncoder
from django.db import connection
from django.contrib.auth.hashers import make_password, check_password


def index(request):
    return JsonResponse({"status": "ok"})


def _run_query(query, fetch, params=None):
    with connection.cursor() as cursor:
        cursor.execute(query, params) if params else cursor.execute(query)
        if fetch:
            columns = [col[0] for col in cursor.description]
            rows = [dict(zip(columns, row)) for row in cursor.fetchall()]
        else:
            rows = []
    return JsonResponse({"queryset": rows}, encoder=DjangoJSONEncoder)


def _parse_params(request):
    raw = request.POST.get('params', None)
    return json.loads(raw) if raw else None


@csrf_exempt
def post_select(request):
    if request.method == 'POST':
        return _run_query(request.POST.get('query', ''), fetch=True, params=_parse_params(request))


@csrf_exempt
def post_insert(request):
    if request.method == 'POST':
        return _run_query(request.POST.get('query', ''), fetch=False, params=_parse_params(request))


@csrf_exempt
def post_remove(request):
    if request.method == 'POST':
        return _run_query(request.POST.get('query', ''), fetch=False, params=_parse_params(request))


@csrf_exempt
def post_update(request):
    if request.method == 'POST':
        return _run_query(request.POST.get('query', ''), fetch=False, params=_parse_params(request))


@csrf_exempt
def login(request):
    """Dedicated login endpoint — verifies hashed password, never exposes raw SQL."""
    if request.method == 'POST':
        email = request.POST.get('email', '')
        password = request.POST.get('password', '')
        with connection.cursor() as cursor:
            cursor.execute("SELECT id, password FROM users WHERE email = %s", [email])
            row = cursor.fetchone()
        if row and check_password(password, row[1]):
            return JsonResponse({"queryset": [{"id": row[0]}]})
        return JsonResponse({"queryset": []})


@csrf_exempt
def register(request):
    """Dedicated register endpoint — stores a hashed password, returns the new user id."""
    if request.method == 'POST':
        username = request.POST.get('username', '')
        name = request.POST.get('name', '')
        surname = request.POST.get('surname', '')
        email = request.POST.get('email', '')
        password = request.POST.get('password', '')
        hashed = make_password(password)
        with connection.cursor() as cursor:
            cursor.execute(
                "INSERT INTO users (username, name, surname, email, password) VALUES (%s, %s, %s, %s, %s)",
                [username, name, surname, email, hashed]
            )
            user_id = cursor.lastrowid
        return JsonResponse({"queryset": [{"id": user_id}]}, encoder=DjangoJSONEncoder)
