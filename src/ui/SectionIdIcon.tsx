import React from "react";
import { SectionId } from "../domain";

export function SectionIdIcon({
    sectionId,
    size = 28,
    title
}: {
    sectionId: SectionId,
    size?: number,
    title?: string
}) {
    return (
        <div
            title={title}
            style={{
                display: 'inline-block',
                width: size,
                height: size,
                backgroundImage: `url(${process.env.PUBLIC_URL}/images/sectionids/${sectionId}.png)`,
                backgroundSize: size
            }}
        />
    );
}