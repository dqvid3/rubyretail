from django.urls import path, include, re_path
from django.views.static import serve
from django.conf import settings

urlpatterns = [
    path('webmobile/', include('webmobile.urls')),
    # Serve product images: /media/images/...
    re_path(r'^webmobile/media/(?P<path>.*)$', serve, {'document_root': settings.MEDIA_ROOT}),
]
