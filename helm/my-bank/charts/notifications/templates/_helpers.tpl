{{- define "notifications.fullname" -}}
{{ .Release.Name }}-notifications
{{- end -}}

{{- define "notifications.labels" -}}
app.kubernetes.io/name: notifications
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "notifications.selectorLabels" -}}
app.kubernetes.io/name: notifications
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}