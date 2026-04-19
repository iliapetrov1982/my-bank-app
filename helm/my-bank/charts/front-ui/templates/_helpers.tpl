{{- define "front-ui.fullname" -}}
{{ .Release.Name }}-front-ui
{{- end -}}

{{- define "front-ui.labels" -}}
app.kubernetes.io/name: front-ui
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "front-ui.selectorLabels" -}}
app.kubernetes.io/name: front-ui
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}