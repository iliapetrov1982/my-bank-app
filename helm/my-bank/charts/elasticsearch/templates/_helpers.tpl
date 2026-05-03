{{- define "elasticsearch.fullname" -}}
{{ .Release.Name }}-elasticsearch
{{- end -}}

{{- define "elasticsearch.labels" -}}
app.kubernetes.io/name: elasticsearch
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "elasticsearch.selectorLabels" -}}
app.kubernetes.io/name: elasticsearch
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}
