{{- define "kibana.fullname" -}}
{{ .Release.Name }}-kibana
{{- end -}}

{{- define "kibana.labels" -}}
app.kubernetes.io/name: kibana
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "kibana.selectorLabels" -}}
app.kubernetes.io/name: kibana
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}
