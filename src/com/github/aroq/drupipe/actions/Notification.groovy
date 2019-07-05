package com.github.aroq.drupipe.actions

class Notification extends BaseAction {

    def email() {
        def email_subject = (action.params.subject && action.params.subject.length() != 0) ? "${action.params.subject}" : ""
        def email_body = (action.params.body && action.params.body.length() != 0) ? "${action.params.body}" : ""
        def email_attach_log = (action.params.attach_log && action.params.attach_log.length() != 0) ? "${action.params.attach_log}" : ""
        def email_attachments_pattern = (action.params.attachments_pattern && action.params.attachments_pattern.length() != 0) ? "${action.params.attachments_pattern}" : ""
        def email_compress_log = action.params.compress_log
        def email_from = (action.params.from && action.params.from.length() != 0) ? "${action.params.from}" : ""
        def email_mime_type = (action.params.mime_type && action.params.mime_type.length() != 0) ? "${action.params.mime_type}" : ""
        def email_reply_to = (action.params.reply_to && action.params.reply_to.length() != 0) ? "${action.params.reply_to}" : ""
        def email_to = (action.params.to && action.params.to.length() != 0) ? "${action.params.to}" : ""
        try {
            this.script.emailext subject: email_subject, body: email_body, attachLog: email_attach_log, attachmentsPattern: email_attachments_pattern, compressLog: email_compress_log, from: email_from, mimeType: email_mime_type, replyTo: email_reply_to, to: email_to
        }
        catch (e) {
            this.script.echo "Email-Ext plugin isn't installed. Use artifact instead."
        }
    }

}

