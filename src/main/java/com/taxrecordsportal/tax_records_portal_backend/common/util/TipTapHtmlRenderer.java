package com.taxrecordsportal.tax_records_portal_backend.common.util;

import java.util.List;
import java.util.Map;

/**
 * Converts TipTap JSON (basic formatting: bold, italic, link, paragraphs, bullet/ordered lists)
 * into HTML string for email rendering.
 */
public final class TipTapHtmlRenderer {

    private TipTapHtmlRenderer() {}

    @SuppressWarnings("unchecked")
    public static String render(Map<String, Object> doc) {
        if (doc == null) return "";
        List<Map<String, Object>> content = (List<Map<String, Object>>) doc.get("content");
        if (content == null) return "";
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> node : content) {
            sb.append(renderNode(node));
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static String renderNode(Map<String, Object> node) {
        String type = (String) node.get("type");
        if (type == null) return "";

        return switch (type) {
            case "paragraph" -> "<p>" + renderContent(node) + "</p>";
            case "heading" -> {
                int level = node.containsKey("attrs")
                        ? ((Number) ((Map<String, Object>) node.get("attrs")).get("level")).intValue()
                        : 1;
                yield "<h" + level + ">" + renderContent(node) + "</h" + level + ">";
            }
            case "bulletList" -> "<ul>" + renderContent(node) + "</ul>";
            case "orderedList" -> "<ol>" + renderContent(node) + "</ol>";
            case "listItem" -> "<li>" + renderContent(node) + "</li>";
            case "blockquote" -> "<blockquote>" + renderContent(node) + "</blockquote>";
            case "hardBreak" -> "<br>";
            case "horizontalRule" -> "<hr>";
            case "text" -> renderText(node);
            default -> renderContent(node);
        };
    }

    @SuppressWarnings("unchecked")
    private static String renderContent(Map<String, Object> node) {
        List<Map<String, Object>> content = (List<Map<String, Object>>) node.get("content");
        if (content == null) return "";
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> child : content) {
            sb.append(renderNode(child));
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static String renderText(Map<String, Object> node) {
        String text = (String) node.get("text");
        if (text == null) return "";

        String escaped = escapeHtml(text);

        List<Map<String, Object>> marks = (List<Map<String, Object>>) node.get("marks");
        if (marks == null) return escaped;

        for (Map<String, Object> mark : marks) {
            String markType = (String) mark.get("type");
            escaped = switch (markType) {
                case "bold" -> "<strong>" + escaped + "</strong>";
                case "italic" -> "<em>" + escaped + "</em>";
                case "underline" -> "<u>" + escaped + "</u>";
                case "strike" -> "<s>" + escaped + "</s>";
                case "link" -> {
                    Map<String, Object> attrs = (Map<String, Object>) mark.get("attrs");
                    String href = attrs != null ? (String) attrs.get("href") : "#";
                    yield "<a href=\"" + sanitizeHref(href) + "\">" + escaped + "</a>";
                }
                default -> escaped;
            };
        }

        return escaped;
    }

    private static String sanitizeHref(String href) {
        if (href == null) return "#";
        String lower = href.trim().toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("mailto:")) {
            return escapeHtml(href);
        }
        return "#";
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
