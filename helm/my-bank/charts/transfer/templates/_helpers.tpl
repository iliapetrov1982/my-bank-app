{{- define "transfer.fullname" -}}
{{ .Release.Name }}-transfer
{{- end -}}

{{- define "transfer.labels" -}}
app.kubernetes.io/name: transfer
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "transfer.selectorLabels" -}}
app.kubernetes.io/name: transfer
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}