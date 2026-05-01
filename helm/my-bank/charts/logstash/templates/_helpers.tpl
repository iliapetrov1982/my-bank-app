{{- define "logstash.fullname" -}}
{{ .Release.Name }}-logstash
{{- end -}}

{{- define "logstash.labels" -}}
app.kubernetes.io/name: logstash
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "logstash.selectorLabels" -}}
app.kubernetes.io/name: logstash
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}
