{{- define "accounts.fullname" -}}
{{ .Release.Name }}-accounts
{{- end -}}

{{- define "accounts.labels" -}}
app.kubernetes.io/name: accounts
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "accounts.selectorLabels" -}}
app.kubernetes.io/name: accounts
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}