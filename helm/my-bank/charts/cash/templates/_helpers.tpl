{{- define "cash.fullname" -}}
{{ .Release.Name }}-cash
{{- end -}}

{{- define "cash.labels" -}}
app.kubernetes.io/name: cash
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "cash.selectorLabels" -}}
app.kubernetes.io/name: cash
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}