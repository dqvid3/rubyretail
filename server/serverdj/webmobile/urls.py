from django.urls import path
from . import views

urlpatterns = [
    path('', views.index, name='index'),
    path('postSelect/', views.post_select, name='postSelect'),
    path('postInsert/', views.post_insert, name='postInsert'),
    path('postRemove/', views.post_remove, name='postRemove'),
    path('postUpdate/', views.post_update, name='postUpdate'),
    path('login/', views.login, name='login'),
    path('register/', views.register, name='register'),
]
