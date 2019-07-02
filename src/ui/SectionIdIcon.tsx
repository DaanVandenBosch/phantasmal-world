import React from "react";
import { SectionId } from "../domain";

export function SectionIdIcon({
    section_id,
    size = 28,
    title,
}: {
    section_id: SectionId;
    size?: number;
    title?: string;
}): JSX.Element {
    return (
        <div
            title={title}
            style={{
                display: "inline-block",
                width: size,
                height: size,
                backgroundImage: `url(${process.env.PUBLIC_URL}/images/sectionids/${SectionId[section_id]}.png)`,
                backgroundSize: size,
            }}
        />
    );
}
