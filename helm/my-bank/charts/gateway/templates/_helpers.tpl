{{- define "gateway.fullname" -}}
{{ .Release.Name }}-gateway
{{- end -}}

{{- define "gateway.labels" -}}
app.kubernetes.io/name: gateway
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "gateway.selectorLabels" -}}
app.kubernetes.io/name: gateway
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}