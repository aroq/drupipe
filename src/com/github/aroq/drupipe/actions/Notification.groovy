package com.github.aroq.drupipe.actions

class Notification extends BaseAction {

    def email() {
        def email_subject = (action.params.email_subject && action.params.email_subject.length() != 0) ? "${action.params.email_subject}" : ""
        def email_body = (action.params.email_body && action.params.email_body.length() != 0) ? "${action.params.email_body}" : ""
        def email_attach_log = (action.params.email_attach_log) ? action.params.email_attach_log : false
        def email_attachments_pattern = (action.params.email_attachments_pattern && action.params.email_attachments_pattern.length() != 0) ? "${action.params.email_attachments_pattern}" : ""
        def email_compress_log = (action.params.email_compress_log) ? action.params.email_compress_log : false
        def email_from = (action.params.email_from && action.params.email_from.length() != 0) ? "${action.params.email_from}" : ""
        def email_mime_type = (action.params.email_mime_type && action.params.email_mime_type.length() != 0) ? "${action.params.email_mime_type}" : ""
        def email_reply_to = (action.params.email_reply_to && action.params.email_reply_to.length() != 0) ? "${action.params.email_reply_to}" : ""
        def email_to = (action.params.email_to && action.params.email_to.length() != 0) ? "${action.params.email_to}" : ""
        try {
            this.script.emailext subject: email_subject, body: email_body, attachLog: email_attach_log, attachmentsPattern: email_attachments_pattern, compressLog: email_compress_log, from: email_from, mimeType: email_mime_type, replyTo: email_reply_to, to: email_to
        }
        catch (e) {
            this.script.echo "Email-Ext plugin isn't installed."
            this.script.echo e.toString()
            this.script.echo e.getMessage()
            this.script.echo e.getStackTrace()
        }
    }

}

