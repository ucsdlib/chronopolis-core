%define __jar_repack {%nil}
%define _prefix %{_usr}/local/chronopolis/ingest
%define jar ingest-server.jar
%define yaml application.yml
%define service /usr/lib/systemd/system/ingest-server.service
%define build_time %(date +"%Y%m%d")

Name: ingest-server
Version: %{ver}
Release: %{build_time}.el7
Source: ingest-server.service
Source1: ingest-server.jar
Source2: application.yml
Summary: Chronopolis Ingest Server
License: UMD
URL: https://gitlab.umiacs.umd.edu/chronopolis
Group: System Environment/Daemons
Requires: postgresql-server >= 8.1
autoprov: yes
autoreq: yes
BuildArch: noarch
BuildRoot: ${_tmppath}/build-%{name}-%{version}

%description
The Ingest Server hosts the API for handling bags, transfers, and
tokens.

%preun

systemctl disable ingest-server

%install

%__install -D -m0644 "%{SOURCE0}" "%{buildroot}%{service}"
%__install -D -m0644 "%{SOURCE1}" "%{buildroot}%{_prefix}/%{jar}"
%__install -D -m0644 "%{SOURCE2}" "%{buildroot}%{_prefix}/%{yaml}"

%__install -d "%{buildroot}/var/log/chronopolis"

%files

%defattr(-,root,root)

%dir %{_prefix}
%dir %attr(0755,-,-) /var/log/chronopolis

%{service}
%{_prefix}/%{jar}
%config(noreplace) %{_prefix}/%{yaml}

%changelog

* Mon Oct 2 2017 Mike Ritter <shake@umiacs.umd.edu> 1.6.0-20171002
- added changelog entry
- update mod for application yaml
